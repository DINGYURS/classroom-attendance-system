import logging
import os
from typing import List, Optional, Tuple

import numpy as np
from insightface.app import FaceAnalysis

from app.config import settings

logger = logging.getLogger(__name__)


class FaceEngine:
    _instance: Optional["FaceEngine"] = None

    def __new__(cls) -> "FaceEngine":
        if cls._instance is None:
            cls._instance = super().__new__(cls)
            cls._instance._initialized = False
        return cls._instance

    def __init__(self) -> None:
        if self._initialized:
            return

        self._initialized = True
        self._face_analyzer: Optional[FaceAnalysis] = None
        self._use_yolo = False
        self._yolo_model = None

        self._load_models()

    def _load_models(self) -> None:
        model_path = settings.yolo_model_path
        if os.path.exists(model_path):
            try:
                from ultralytics import YOLO

                logger.info(f"Loading YOLO face detection model from {model_path}...")
                self._yolo_model = YOLO(model_path)
                self._use_yolo = True
                logger.info("YOLO model loaded successfully")
            except Exception as e:
                logger.warning(f"Failed to load YOLO model: {e}, will use InsightFace detector")
                self._use_yolo = False
        else:
            logger.info(f"YOLO model not found at {model_path}, using InsightFace detector")
            self._use_yolo = False

        logger.info("Loading InsightFace model...")
        try:
            self._face_analyzer = FaceAnalysis(
                name=settings.insightface_model_name,
                providers=["CUDAExecutionProvider", "CPUExecutionProvider"],
            )
            self._face_analyzer.prepare(ctx_id=0, det_size=(640, 640))
            used_providers = self._face_analyzer.models[
                list(self._face_analyzer.models.keys())[0]
            ].session.get_providers()
            mode = "GPU" if "CUDAExecutionProvider" in used_providers else "CPU"
            logger.info(f"InsightFace model loaded successfully ({mode} mode)")
        except Exception as e:
            logger.error(f"Failed to load InsightFace model: {e}")
            raise

    def detect_faces_yolo(self, image: np.ndarray) -> List[Tuple[int, int, int, int, float]]:
        if not self._use_yolo or self._yolo_model is None:
            raise RuntimeError("YOLO model not available")

        predict_kwargs = {
            "conf": settings.face_detection_confidence,
            "imgsz": settings.yolo_inference_imgsz,
            "augment": settings.yolo_inference_augment,
            "verbose": False,
        }
        logger.info(
            "[FACE-DETECT-101] start_yolo_detect image_shape=%s conf=%.3f imgsz=%d augment=%s",
            tuple(image.shape),
            settings.face_detection_confidence,
            settings.yolo_inference_imgsz,
            settings.yolo_inference_augment,
        )
        results = self._yolo_model(image, **predict_kwargs)

        faces = []
        for result in results:
            boxes = result.boxes
            if boxes is None:
                continue

            for i in range(len(boxes)):
                box = boxes.xyxy[i].cpu().numpy()
                conf = float(boxes.conf[i].cpu().numpy())
                x1, y1, x2, y2 = map(int, box)
                faces.append((x1, y1, x2, y2, conf))

        logger.info("[FACE-DETECT-102] yolo_detect_done raw_face_count=%d", len(faces))
        return faces[:settings.max_faces_per_image]

    def _clip_box(
        self, box: Tuple[int, int, int, int], image_shape: Tuple[int, ...]
    ) -> Optional[Tuple[int, int, int, int]]:
        height, width = image_shape[:2]
        x1, y1, x2, y2 = box

        x1 = max(0, min(x1, width - 1))
        y1 = max(0, min(y1, height - 1))
        x2 = max(0, min(x2, width))
        y2 = max(0, min(y2, height))

        if x2 <= x1 or y2 <= y1:
            return None

        return x1, y1, x2, y2

    def _expand_box(
        self,
        box: Tuple[int, int, int, int],
        image_shape: Tuple[int, ...],
        expand_ratio: float,
    ) -> Optional[Tuple[int, int, int, int]]:
        x1, y1, x2, y2 = box
        width = x2 - x1
        height = y2 - y1
        pad_x = int(width * expand_ratio)
        pad_y = int(height * expand_ratio)
        expanded_box = (x1 - pad_x, y1 - pad_y, x2 + pad_x, y2 + pad_y)
        return self._clip_box(expanded_box, image_shape)

    def extract_features_insightface(self, image: np.ndarray) -> List[dict]:
        if self._face_analyzer is None:
            raise RuntimeError("InsightFace model not loaded")

        faces = self._face_analyzer.get(image)

        results = []
        for face in faces[:settings.max_faces_per_image]:
            bbox = face.bbox.astype(int).tolist()
            embedding = face.embedding.tolist() if face.embedding is not None else None

            results.append(
                {
                    "bbox": bbox,
                    "embedding": embedding,
                    "det_score": float(face.det_score) if hasattr(face, "det_score") else 0.0,
                }
            )

        return results

    def extract_single_face_feature(self, image: np.ndarray) -> Optional[List[float]]:
        faces = self.extract_features_insightface(image)

        if not faces:
            return None

        if len(faces) > 1:
            logger.warning(f"Multiple faces detected ({len(faces)}), using the largest one")
            largest_face = max(
                faces,
                key=lambda f: (f["bbox"][2] - f["bbox"][0]) * (f["bbox"][3] - f["bbox"][1]),
            )
            return largest_face.get("embedding")

        return faces[0].get("embedding")

    def detect_and_extract_all(self, image: np.ndarray) -> List[dict]:
        if self._use_yolo and self._yolo_model is not None:
            yolo_faces = self.detect_faces_yolo(image)
            results = []
            embedding_success_count = 0
            embedding_missing_count = 0
            expand_ratios = [
                settings.yolo_crop_expand_ratio,
                settings.yolo_crop_retry_expand_ratio,
            ]

            logger.info("[FACE-DETECT-103] detect_pipeline_yolo_boxes=%d", len(yolo_faces))

            for index, (x1, y1, x2, y2, det_score) in enumerate(yolo_faces, start=1):
                original_box = self._clip_box((x1, y1, x2, y2), image.shape)
                if original_box is None:
                    logger.warning(
                        "[FACE-DETECT-104] skip_invalid_box index=%d box=%s image_shape=%s",
                        index,
                        (x1, y1, x2, y2),
                        tuple(image.shape),
                    )
                    continue

                embedding = None
                chosen_box = original_box
                for attempt, expand_ratio in enumerate(expand_ratios, start=1):
                    expanded_box = self._expand_box(original_box, image.shape, expand_ratio)
                    if expanded_box is None:
                        logger.warning(
                            "[FACE-DETECT-105] skip_invalid_expanded_box index=%d attempt=%d box=%s expand_ratio=%.2f",
                            index,
                            attempt,
                            original_box,
                            expand_ratio,
                        )
                        continue

                    cx1, cy1, cx2, cy2 = expanded_box
                    face_crop = image[cy1:cy2, cx1:cx2]
                    if face_crop.size == 0:
                        logger.warning(
                            "[FACE-DETECT-105] skip_empty_crop index=%d attempt=%d box=%s expanded_box=%s",
                            index,
                            attempt,
                            original_box,
                            expanded_box,
                        )
                        continue

                    insight_faces = self.extract_features_insightface(face_crop)
                    logger.info(
                        "[FACE-DETECT-106] crop_analyzed index=%d attempt=%d expand_ratio=%.2f yolo_box=%s crop_box=%s crop_shape=%s insight_faces=%d",
                        index,
                        attempt,
                        expand_ratio,
                        original_box,
                        expanded_box,
                        tuple(face_crop.shape),
                        len(insight_faces),
                    )
                    if not insight_faces:
                        continue

                    best_face = max(
                        insight_faces,
                        key=lambda face: (
                            float(face.get("det_score", 0.0)),
                            (face["bbox"][2] - face["bbox"][0]) * (face["bbox"][3] - face["bbox"][1]),
                        ),
                    )
                    embedding = best_face.get("embedding")
                    chosen_box = expanded_box
                    if embedding is not None:
                        embedding_success_count += 1
                        break

                    logger.warning(
                        "[FACE-DETECT-107] embedding_missing_after_best_face index=%d attempt=%d crop_box=%s",
                        index,
                        attempt,
                        expanded_box,
                    )

                if embedding is None:
                    embedding_missing_count += 1
                    logger.warning(
                        "[FACE-DETECT-108] no_embedding_after_all_attempts index=%d yolo_box=%s",
                        index,
                        original_box,
                    )

                results.append(
                    {
                        "bbox": list(original_box),
                        "embedding": embedding,
                        "det_score": det_score,
                    }
                )

            logger.info(
                "[FACE-DETECT-109] detect_pipeline_done returned_faces=%d embedding_success=%d embedding_missing=%d",
                len(results),
                embedding_success_count,
                embedding_missing_count,
            )
            return results[:settings.max_faces_per_image]

        logger.warning("[FACE-DETECT-110] yolo_unavailable_fallback_to_insightface")
        return self.extract_features_insightface(image)


def get_face_engine() -> FaceEngine:
    return FaceEngine()