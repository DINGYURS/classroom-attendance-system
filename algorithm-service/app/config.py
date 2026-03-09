from functools import lru_cache
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    app_name: str = "Face Recognition Algorithm Service"
    app_version: str = "1.0.0"
    debug: bool = False
    host: str = "0.0.0.0"
    port: int = 8000

    minio_endpoint: str = "localhost:9000"
    minio_access_key: str = "RollCallSystem"
    minio_secret_key: str = "12345678"
    minio_bucket_name: str = "rollcall"
    minio_secure: bool = False

    yolo_model_path: str = "weights/best-1280.pt"
    insightface_model_name: str = "buffalo_l"

    face_detection_confidence: float = 0.5
    yolo_inference_imgsz: int = 1280
    yolo_inference_augment: bool = False
    yolo_crop_expand_ratio: float = 0.35
    yolo_crop_retry_expand_ratio: float = 0.65
    face_recognition_threshold: float = 0.6
    max_faces_per_image: int = 100

    temp_dir: str = "/tmp/face_service"

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


@lru_cache()
def get_settings() -> Settings:
    return Settings()


settings = get_settings()