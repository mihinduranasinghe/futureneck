import cv2
import io
from googleapiclient.discovery import build
from google.oauth2 import service_account
from google.cloud import translate_v2 as translate

import base64
import openai

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

# Function to detect text in an image
def detect_text(image_content, service):
    encoded_image = base64.b64encode(image_content).decode('UTF-8')

    service_request = service.images().annotate(body={
        'requests': [{
            'image': {
                'content': encoded_image
            },
            'features': [{
                'type': 'TEXT_DETECTION'
            }]
        }]
    })
    response = service_request.execute()
    text_annotations = response['responses'][0].get('textAnnotations', [])
    
    if text_annotations:
        return text_annotations[0]['description']
    else:
        return None

# Function to read the image file
def read_image_file(file_path):
    with io.open(file_path, 'rb') as image_file:
        content = image_file.read()
    return content

def process_with_openai(text):
    # Set the OpenAI API key
    openai_api_key = "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"
    openai.api_key = openai_api_key

    # Create the OpenAI client
    client = openai.OpenAI()

    # Create a completion request
    completion = client.chat.completions.create(
        model="gpt-3.5-turbo",
        messages=[
            {"role": "system", "content": text},
            {"role": "user", "content": "Please check this and give an answer."}
        ]
    )
    # Print the response
    print(completion.choices[0].message)

def main():
    key_file_path = '/home/pi/futureneck/secrets.json'
    scopes = ['https://www.googleapis.com/auth/cloud-platform']

    credentials = service_account.Credentials.from_service_account_file(key_file_path, scopes=scopes)
    service = build('vision', 'v1', credentials=credentials)

    image_path = capture_image_from_webcam()
    if image_path:
        image_content = read_image_file(image_path)
        detected_text = detect_text(image_content, service)
        if detected_text:
            print("Detected Text:", detected_text)
            # Process the detected text with OpenAI
            print("AI Response:", process_with_openai(detected_text))
        else:
            print("No text detected")

if __name__ == '__main__':
    main()