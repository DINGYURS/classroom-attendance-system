import logging
from typing import Optional

import cv2
import numpy as np
from minio import Minio

from app.config import settings

logger = logging.getLogger(__name__)


class ImageDownloader:
    def __init__(self) -> None:
        self._minio_client: Optional[Minio] = None
        self._init_minio_client()

    def _init_minio_client(self) -> None:
        try:
            self._minio_client = Minio(
                endpoint=settings.minio_endpoint,
                access_key=settings.minio_access_key,
                secret_key=settings.minio_secret_key,
                secure=settings.minio_secure,
            )
            logger.info(f"MinIO client initialized: {settings.minio_endpoint}")
        except Exception as e:
            logger.error(f"Failed to initialize MinIO client: {e}")
            self._minio_client = None

    def download_image_by_object_key(self, object_key: str) -> Optional[np.ndarray]:
        if self._minio_client is None:
            logger.error("MinIO client not initialized")
            return None

        if not object_key:
            logger.error("Invalid MinIO object key: %s", object_key)
            return None

        try:
            response = self._minio_client.get_object(settings.minio_bucket_name, object_key)
            image_data = response.read()
            response.close()
            response.release_conn()
            return self._decode_image(image_data, f"minio:{settings.minio_bucket_name}/{object_key}")
        except Exception as e:
            logger.error(f"Failed to download from MinIO by object key: {object_key}, error: {e}")
            return None

    def _decode_image(self, image_bytes: bytes, source: str) -> Optional[np.ndarray]:
        nparr = np.frombuffer(image_bytes, np.uint8)
        image = cv2.imdecode(nparr, cv2.IMREAD_COLOR)

        if image is None:
            logger.error(f"Failed to decode image from {source}")
            return None

        # 保持 OpenCV 默认的 BGR 格式。
        # 本地手工测试脚本使用 cv2.imread()，返回的也是 BGR；服务端必须保持一致。
        logger.info("[FACE-DETECT-001] image_decoded source=%s image_shape=%s color_space=BGR", source, tuple(image.shape))
        return image

_downloader_instance: Optional[ImageDownloader] = None


def get_image_downloader() -> ImageDownloader:
    global _downloader_instance
    if _downloader_instance is None:
        _downloader_instance = ImageDownloader()
    return _downloader_instance
