from typing import List, Optional, Any
from pydantic import BaseModel, Field


class ApiResponse(BaseModel):
    code: int = Field(default=1)  # 1=成功, 0=失败
    msg: str = Field(default="success")
    data: Optional[Any] = None


# ── /api/face/extract ──────────────────────────────────────────────────────────

class ExtractRequest(BaseModel):
    """单人人脸特征提取请求"""
    objectKey: str = Field(..., description="MinIO 对象键")


# ── /api/face/detect ───────────────────────────────────────────────────────────

class DetectRequest(BaseModel):
    """合照人脸检测请求（考勤用）"""
    objectKeys: List[str] = Field(..., description="合照对象键列表")


class FaceInfo(BaseModel):
    """单张人脸信息"""
    bbox: List[int] = Field(..., description="人脸边界框 [x1, y1, x2, y2]")
    embedding: Optional[List[float]] = Field(None, description="512 维特征向量，Java 用于余弦相似度比对")
    detScore: float = Field(default=0.0, description="人脸检测置信度，仅表示该区域像人脸，不代表身份识别相似度")


class DetectResponse(BaseModel):
    """单张图片的所有人脸检测结果"""
    imageIndex: int
    faces: List[FaceInfo]
