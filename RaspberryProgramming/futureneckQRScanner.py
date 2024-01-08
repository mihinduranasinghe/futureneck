import cv2
import io
from pyzbar.pyzbar import decode

import base64

# Function to capture an image from the webcam
def capture_image_from_webcam():
    cam = cv2.VideoCapture(0)
    ret, frame = cam.read()
    if not ret:
        print("Failed to capture image")
        cam.release()
        return None
    cv2.imwrite('captured_image.jpg', frame)
    cam.release()
    return 'captured_image.jpg'

# Function to scan QR code
def scan_barcode_qr(image_path):
    image = cv2.imread(image_path)
    decoded_objects = decode(image)
    for obj in decoded_objects:
        print("Type:", obj.type)
        print("Data:", obj.data.decode("utf-8"))

# Function to read the image file
def read_image_file(file_path):
    with io.open(file_path, 'rb') as image_file:
        content = image_file.read()
    return content

def main():
    # Capture an image from the webcam
    image_path = capture_image_from_webcam()
    if image_path:
        scan_barcode_qr(image_path)

if __name__ == '__main__':
    main()