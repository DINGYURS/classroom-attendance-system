import json
import logging
from typing import List

import numpy as np
from fastapi import APIRouter, HTTPException

from app.config import settings
from app.core.downloader import get_image_downloader
from app.core.engine import get_face_engine
from app.schemas import (
    ApiResponse,
    ExtractRequest,
    DetectRequest,
    DetectResponse,
    FaceInfo,
    RecognizeRequest,
    RecognitionMatch,
)

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/face", tags=["Face Recognition"])


@router.post("/extract")
async def extract_face_feature(request: ExtractRequest) -> ApiResponse:
    try:
        downloader = get_image_downloader()
        image = downloader.download_image(request.imageUrl)
        
        if image is None:
            return ApiResponse(code=1, msg="Failed to download image", data=None)
        
        engine = get_face_engine()
        feature = engine.extract_single_face_feature(image)
        
        if feature is None:
            return ApiResponse(code=1, msg="No face detected in the image", data=None)
        
        feature_json = json.dumps(feature)
        return ApiResponse(code=0, msg="success", data=feature_json)
        
    except Exception as e:
        logger.error(f"Extract face feature failed: {e}")
        return ApiResponse(code=1, msg=str(e), data=None)


@router.post("/detect")
async def detect_faces(request: DetectRequest) -> ApiResponse:
    try:
        downloader = get_image_downloader()
        engine = get_face_engine()
        
        results: List[DetectResponse] = []
        
        for idx, image_url in enumerate(request.imageUrls):
            image = downloader.download_image(image_url)
            
            if image is None:
                logger.warning(f"Failed to download image: {image_url}")
                results.append(DetectResponse(imageIndex=idx, faces=[]))
                continue
            
            faces_data = engine.detect_and_extract_all(image)
            
            faces = []
            for face in faces_data:
                faces.append(FaceInfo(
                    bbox=face["bbox"],
                    embedding=face.get("embedding"),
                    detScore=face.get("det_score", 0.0)
                ))
            
            results.append(DetectResponse(imageIndex=idx, faces=faces))
        
        return ApiResponse(code=0, msg="success", data=[r.model_dump() for r in results])
        
    except Exception as e:
        logger.error(f"Detect faces failed: {e}")
        return ApiResponse(code=1, msg=str(e), data=None)


@router.post("/recognize")
async def recognize_faces(request: RecognizeRequest) -> ApiResponse:
    try:
        downloader = get_image_downloader()
        engine = get_face_engine()
        
        all_detected_faces = []
        
        for image_url in request.imageUrls:
            image = downloader.download_image(image_url)
            if image is None:
                continue
            
            faces_data = engine.detect_and_extract_all(image)
            all_detected_faces.extend(faces_data)
        
        student_features = {}
        for sf in request.studentFeatures:
            try:
                feature_list = json.loads(sf.featureVector)
                student_features[sf.studentId] = np.array(feature_list, dtype=np.float32)
            except Exception as e:
                logger.warning(f"Failed to parse feature for student {sf.studentId}: {e}")
        
        matches: List[RecognitionMatch] = []
        matched_student_ids = set()
        unmatched_count = 0
        
        for face_data in all_detected_faces:
            embedding = face_data.get("embedding")
            if embedding is None:
                unmatched_count += 1
                continue
            
            face_embedding = np.array(embedding, dtype=np.float32)
            
            best_match_id = None
            best_similarity = 0.0
            
            for student_id, student_embedding in student_features.items():
                if student_id in matched_student_ids:
                    continue
                
                similarity = _cosine_similarity(face_embedding, student_embedding)
                
                if similarity > settings.face_recognition_threshold and similarity > best_similarity:
                    best_similarity = similarity
                    best_match_id = student_id
            
            if best_match_id is not None:
                matched_student_ids.add(best_match_id)
                matches.append(RecognitionMatch(
                    studentId=best_match_id,
                    similarity=float(best_similarity),
                    matched=True,
                    bbox=face_data.get("bbox")
                ))
            else:
                unmatched_count += 1
        
        return ApiResponse(
            code=0,
            msg="success",
            data=[m.model_dump() for m in matches]
        )
        
    except Exception as e:
        logger.error(f"Recognize faces failed: {e}")
        return ApiResponse(code=1, msg=str(e), data=None)


def _cosine_similarity(vec1: np.ndarray, vec2: np.ndarray) -> float:
    norm1 = np.linalg.norm(vec1)
    norm2 = np.linalg.norm(vec2)
    
    if norm1 == 0 or norm2 == 0:
        return 0.0
    
    return float(np.dot(vec1, vec2) / (norm1 * norm2))
