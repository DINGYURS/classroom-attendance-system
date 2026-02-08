import os
import urllib.request
import sys


MODELS = {
    "yolov11n-face": {
        "url": "https://huggingface.co/akanametov/yolo11-face/resolve/main/yolov11n-face.pt",
        "filename": "yolov11n-face.pt"
    }
}


def download_file(url: str, destination: str) -> None:
    print(f"Downloading from {url}")
    print(f"Saving to {destination}")
    
    def progress_hook(block_num, block_size, total_size):
        downloaded = block_num * block_size
        if total_size > 0:
            percent = min(100, downloaded * 100 / total_size)
            sys.stdout.write(f"\rProgress: {percent:.1f}%")
            sys.stdout.flush()
    
    urllib.request.urlretrieve(url, destination, progress_hook)
    print("\nDownload complete!")


def main():
    weights_dir = os.path.join(os.path.dirname(__file__), "weights")
    os.makedirs(weights_dir, exist_ok=True)
    
    print("=" * 50)
    print("Face Recognition Model Downloader")
    print("=" * 50)
    
    for model_name, model_info in MODELS.items():
        dest_path = os.path.join(weights_dir, model_info["filename"])
        
        if os.path.exists(dest_path):
            print(f"\n{model_name}: Already exists at {dest_path}")
            continue
        
        print(f"\nDownloading {model_name}...")
        try:
            download_file(model_info["url"], dest_path)
        except Exception as e:
            print(f"Failed to download {model_name}: {e}")
            continue
    
    print("\n" + "=" * 50)
    print("InsightFace models will be downloaded automatically")
    print("on first run (buffalo_l - ~300MB)")
    print("=" * 50)


if __name__ == "__main__":
    main()
