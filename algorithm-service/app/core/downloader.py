import logging
from typing import Optional
from urllib.parse import urlparse

import cv2
import httpx
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

    def download_image(self, image_url: str) -> Optional[np.ndarray]:
        if self._is_minio_url(image_url):
            return self._download_from_minio(image_url)
        return self._download_from_http(image_url)

    def _is_minio_url(self, url: str) -> bool:
        """
        判断是否为 MinIO 地址。
        若 URL 携带查询参数（预签名 URL），直接走 HTTP 下载即可，无需 SDK。
        """
        parsed = urlparse(url)
        if parsed.query:
            return False
        minio_host = settings.minio_endpoint.split(":")[0]
        return minio_host in parsed.netloc or "minio" in parsed.netloc.lower()

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

    def _download_from_minio(self, url: str) -> Optional[np.ndarray]:
        if self._minio_client is None:
            logger.error("MinIO client not initialized")
            return None

        try:
            parsed = urlparse(url)
            path_parts = parsed.path.strip("/").split("/", 1)

            if len(path_parts) == 2:
                bucket_name, object_name = path_parts
            else:
                bucket_name = settings.minio_bucket_name
                object_name = path_parts[0] if path_parts else ""

            if not object_name:
                logger.error(f"Invalid MinIO URL: {url}")
                return None

            response = self._minio_client.get_object(bucket_name, object_name)
            image_data = response.read()
            response.close()
            response.release_conn()
            return self._decode_image(image_data, f"minio:{bucket_name}/{object_name}")

        except Exception as e:
            logger.error(f"Failed to download from MinIO: {url}, error: {e}")
            return None

    def _download_from_http(self, url: str) -> Optional[np.ndarray]:
        try:
            with httpx.Client(timeout=30.0) as client:
                response = client.get(url)
                response.raise_for_status()
                return self._decode_image(response.content, url)

        except Exception as e:
            logger.error(f"Failed to download from HTTP: {url}, error: {e}")
            return None


_downloader_instance: Optional[ImageDownloader] = None


def get_image_downloader() -> ImageDownloader:
    global _downloader_instance
    if _downloader_instance is None:
        _downloader_instance = ImageDownloader()
    return _downloader_instance