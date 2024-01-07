import cv2
import io
from googleapiclient.discovery import build
from google.oauth2 import service_account
from google.cloud import translate_v2 as translate

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

# Function to read the image file
def read_image_file(file_path):
    with io.open(file_path, 'rb') as image_file:
        content = image_file.read()
    return content

# Function to detect labels in an image
def detect_labels(image_content, service):
    # Convert the image content to base64
    encoded_image = base64.b64encode(image_content).decode('UTF-8')

    service_request = service.images().annotate(body={
        'requests': [{
            'image': {
                'content': encoded_image
            },
            'features': [{
                'type': 'LABEL_DETECTION',
                'maxResults': 10
            }]
        }]
    })
    response = service_request.execute()
    labels = response['responses'][0].get('labelAnnotations', [])
    
    print('Following 10 objects detected in front of you:')
    for label in labels:
        print(label['description'])

def main():
    key_file_path = '/home/pi/futureneck/secrets.json'
    scopes = ['https://www.googleapis.com/auth/cloud-platform']

    # Authenticate and build the service
    credentials = service_account.Credentials.from_service_account_file(key_file_path, scopes=scopes)
    service = build('vision', 'v1', credentials=credentials)

    # Initialize Google Cloud Translation client
    translate_client = translate.Client(credentials=credentials)

    # Capture an image from the webcam
    image_path = capture_image_from_webcam()
    if image_path:
        # Read the captured image and detect labels
        image_content = read_image_file(image_path)
        detect_labels(image_content, service)

if __name__ == '__main__':
    main()