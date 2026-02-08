import io
import logging
import os
import tempfile
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
                secure=settings.minio_secure
            )
            logger.info(f"MinIO client initialized: {settings.minio_endpoint}")
        except Exception as e:
            logger.error(f"Failed to initialize MinIO client: {e}")
            self._minio_client = None
    
    def download_image(self, image_url: str) -> Optional[np.ndarray]:
        if self._is_minio_url(image_url):
            return self._download_from_minio(image_url)
        else:
            return self._download_from_http(image_url)
    
    def _is_minio_url(self, url: str) -> bool:
        parsed = urlparse(url)
        minio_host = settings.minio_endpoint.split(":")[0]
        return minio_host in parsed.netloc or "minio" in parsed.netloc.lower()
    
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
            
            nparr = np.frombuffer(image_data, np.uint8)
            image = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
            
            if image is None:
                logger.error(f"Failed to decode image from MinIO: {url}")
                return None
            
            return cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
            
        except Exception as e:
            logger.error(f"Failed to download from MinIO: {url}, error: {e}")
            return None
    
    def _download_from_http(self, url: str) -> Optional[np.ndarray]:
        try:
            with httpx.Client(timeout=30.0) as client:
                response = client.get(url)
                response.raise_for_status()
                
                nparr = np.frombuffer(response.content, np.uint8)
                image = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
                
                if image is None:
                    logger.error(f"Failed to decode image from HTTP: {url}")
                    return None
                
                return cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
                
        except Exception as e:
            logger.error(f"Failed to download from HTTP: {url}, error: {e}")
            return None


_downloader_instance: Optional[ImageDownloader] = None


def get_image_downloader() -> ImageDownloader:
    global _downloader_instance
    if _downloader_instance is None:
        _downloader_instance = ImageDownloader()
    return _downloader_instance
