import logging
import os
from typing import List, Tuple, Optional

import cv2
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
                providers=['CPUExecutionProvider']  # 使用 CPU
                # providers=['CUDAExecutionProvider', 'CPUExecutionProvider'] # 使用 GPU
            )
            self._face_analyzer.prepare(ctx_id=0, det_size=(640, 640))
            logger.info("InsightFace model loaded successfully (CPU mode)")
        except Exception as e:
            logger.error(f"Failed to load InsightFace model: {e}")
            raise
    
    def detect_faces_yolo(self, image: np.ndarray) -> List[Tuple[int, int, int, int, float]]:
        if not self._use_yolo or self._yolo_model is None:
            raise RuntimeError("YOLO model not available")
        
        results = self._yolo_model(image, conf=settings.face_detection_confidence, verbose=False)
        
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
        
        return faces[:settings.max_faces_per_image]
    
    def extract_features_insightface(self, image: np.ndarray) -> List[dict]:
        if self._face_analyzer is None:
            raise RuntimeError("InsightFace model not loaded")
        
        faces = self._face_analyzer.get(image)
        
        results = []
        for face in faces[:settings.max_faces_per_image]:
            bbox = face.bbox.astype(int).tolist()
            embedding = face.embedding.tolist() if face.embedding is not None else None
            
            results.append({
                "bbox": bbox,
                "embedding": embedding,
                "det_score": float(face.det_score) if hasattr(face, 'det_score') else 0.0
            })
        
        return results
    
    def extract_single_face_feature(self, image: np.ndarray) -> Optional[List[float]]:
        faces = self.extract_features_insightface(image)
        
        if not faces:
            return None
        
        if len(faces) > 1:
            logger.warning(f"Multiple faces detected ({len(faces)}), using the largest one")
            largest_face = max(faces, key=lambda f: (f["bbox"][2] - f["bbox"][0]) * (f["bbox"][3] - f["bbox"][1]))
            return largest_face.get("embedding")
        
        return faces[0].get("embedding")
    
    def detect_and_extract_all(self, image: np.ndarray) -> List[dict]:
        return self.extract_features_insightface(image)


def get_face_engine() -> FaceEngine:
    return FaceEngine()
