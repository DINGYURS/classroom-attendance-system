from typing import List, Optional, Any
from pydantic import BaseModel, Field


class ApiResponse(BaseModel):
    code: int = Field(default=0)
    msg: str = Field(default="success")
    data: Optional[Any] = None


class ExtractRequest(BaseModel):
    imageUrl: str = Field(..., description="Image URL from MinIO")


class ExtractResponse(BaseModel):
    featureVector: Optional[List[float]] = None
    faceCount: int = 0
    bbox: Optional[List[int]] = None


class DetectRequest(BaseModel):
    imageUrls: List[str] = Field(..., description="List of image URLs")


class FaceInfo(BaseModel):
    bbox: List[int] = Field(..., description="Bounding box [x1, y1, x2, y2]")
    embedding: Optional[List[float]] = None
    detScore: float = Field(default=0.0, description="Detection confidence score")


class DetectResponse(BaseModel):
    imageIndex: int
    faces: List[FaceInfo]


class StudentFeature(BaseModel):
    studentId: int
    featureVector: str


class RecognizeRequest(BaseModel):
    imageUrls: List[str] = Field(..., description="List of classroom photo URLs")
    studentFeatures: List[StudentFeature] = Field(..., description="List of student features for matching")


class RecognitionMatch(BaseModel):
    studentId: int
    similarity: float
    matched: bool
    bbox: Optional[List[int]] = None


class RecognizeResponse(BaseModel):
    matches: List[RecognitionMatch]
    totalFacesDetected: int
    unmatchedFaces: int


class HealthResponse(BaseModel):
    status: str
    models_loaded: bool
    yolo_loaded: bool
    insightface_loaded: bool
