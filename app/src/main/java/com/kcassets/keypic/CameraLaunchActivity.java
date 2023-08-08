package com.kcassets.keypic;

import static androidx.camera.core.AspectRatio.RATIO_4_3;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.FileList;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class CameraLaunchActivity extends AppCompatActivity {


    /***********************************************************
     * Initializations
     **********************************************************/
    ImageButton capture, toggleFlash;
    TextView photoNameView;
    String photoName;
    String fileName;
    String folderName;
    Button closeBtn;
    String accessToken;
    private Drive driveService;
    SharedPreferences sharedPreferences;
    private PreviewView previewView;
    private int currentOrientation;
    int cameraFacing = CameraSelector.LENS_FACING_BACK;
    private final ActivityResultLauncher<String> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
        @Override
        public void onActivityResult(Boolean result) {
            if (result) {
                startCamera(cameraFacing);
            }
        }
    });


    /***********************************************************
     * onCreate
     **********************************************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_launcher);


        /***********************************************************
         * Local Initializations and Permissions Check
         **********************************************************/
        previewView = findViewById(R.id.cameraPreview);
        capture = findViewById(R.id.capture);
        toggleFlash = findViewById(R.id.toggleFlash);
        photoNameView = findViewById(R.id.photoName);
        closeBtn = findViewById(R.id.closeBtn);

        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);

        AccessTokenManager accessTokenManager = new AccessTokenManager(this);
        accessTokenManager.checkAccessTokenExpiration();

        // Access the passed photo name and file name from intent extras
        photoName = getIntent().getStringExtra("photoName");
        fileName = getIntent().getStringExtra("fileName");
        folderName = getIntent().getStringExtra("folderName");

        photoNameView.setText(photoName);

        if (ContextCompat.checkSelfPermission(CameraLaunchActivity.this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            activityResultLauncher.launch(android.Manifest.permission.CAMERA);
        } else {
            startCamera(cameraFacing);
        }

        closeBtn.setOnClickListener(v -> {
            Intent intent = new Intent();
            setResult(RESULT_CANCELED, intent);
            finish();
        });
    }


    /***********************************************************
     * Camera Opener
     **********************************************************/
    public void startCamera(int cameraFacing) {
        ListenableFuture<ProcessCameraProvider> listenableFuture = ProcessCameraProvider.getInstance(this);

        listenableFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = (ProcessCameraProvider) listenableFuture.get();

                Preview preview = new Preview.Builder().setTargetAspectRatio(RATIO_4_3).build();

                ImageCapture imageCapture = new ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation()).build();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(cameraFacing).build();

                cameraProvider.unbindAll();

                Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

                capture.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        AccessTokenManager accessTokenManager = new AccessTokenManager(CameraLaunchActivity.this);
                        accessTokenManager.checkAccessTokenExpiration();

                        // Set the output file path and create a file to store the captured image
                        capture.setVisibility(View.GONE);
                        closeBtn.setVisibility(View.GONE);
                        File outputDir = getExternalCacheDir();
                        File outputFile = new File(outputDir, "captured_image.jpg");

                        // Create the output options with the file as the target
                        ImageCapture.OutputFileOptions outputFileOptions =
                                new ImageCapture.OutputFileOptions.Builder(outputFile).build();

                        // Capture the image
                        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(CameraLaunchActivity.this),
                                new ImageCapture.OnImageSavedCallback() {
                                    @Override
                                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                                        // Image capture succeeded
                                        // Display the captured image in a dialog
                                        displayImageDialog(outputFile, photoName);
                                    }

                                    @Override
                                    public void onError(@NonNull ImageCaptureException exception) {
                                        // Image capture failed
                                        Toast.makeText(CameraLaunchActivity.this, "Error capturing image", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                });
                toggleFlash.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        setFlashIcon(camera);
                    }
                });

                preview.setSurfaceProvider(previewView.getSurfaceProvider());
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }


    /***********************************************************
     * Image Display and Saving
     **********************************************************/
    private void displayImageDialog(File imageFile, String photoName) {
        // Create a dialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(CameraLaunchActivity.this);
        currentOrientation = getResources().getConfiguration().orientation;
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        // Set the dialog title
        builder.setTitle("Captured Image");

        // Inflate the custom layout for the dialog content
        View dialogView = getLayoutInflater().inflate(R.layout.image_dialog_layout, null);

        // Set the photo name
        TextView photoNameTextView = dialogView.findViewById(R.id.photoNameTextView);
        photoNameTextView.setText(photoName);

        // Set the captured image
        ImageView capturedImageView = dialogView.findViewById(R.id.capturedImageView);
        capturedImageView.setImageURI(Uri.fromFile(imageFile));
        capturedImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

        // Set the 'save' and 'retake' buttons
        builder.setPositiveButton("Save and Continue", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                AccessTokenManager accessTokenManager = new AccessTokenManager(CameraLaunchActivity.this);
                accessTokenManager.checkAccessTokenExpiration();

                new SaveImageToDriveTask().execute(imageFile);
            }
        });

        builder.setNegativeButton("Retake", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                AccessTokenManager accessTokenManager = new AccessTokenManager(CameraLaunchActivity.this);
                accessTokenManager.checkAccessTokenExpiration();

                deletePhoto(imageFile);
                capture.setVisibility(View.VISIBLE);
                closeBtn.setVisibility(View.VISIBLE);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                // Close dialogue and retake
            }
        });

        // Set the custom layout to the dialog builder
        builder.setView(dialogView);

        // Create and show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    /***********************************************************
     * Remove Image File from Cache
     **********************************************************/
    private void deletePhoto(File photoFile) {
        if (photoFile.exists()) {
            if (photoFile.delete()) {
                // Photo successfully deleted from cache
            } else {
                // Failed to delete the photo from cache
            }
        }
    }


    /***********************************************************
     * Save Image to Drive Folder
     **********************************************************/
    private class SaveImageToDriveTask extends AsyncTask<File, Void, Boolean> {
        private ProgressDialog progressDialog;
        public SaveImageToDriveTask() {
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(CameraLaunchActivity.this);
            progressDialog.setMessage("Saving image to Google Drive...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        }

        @Override
        protected Boolean doInBackground(File... files) {
            sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
            accessToken = sharedPreferences.getString("access_token", null);

            HttpTransport httpTransport = new com.google.api.client.http.javanet.NetHttpTransport();
            JsonFactory jsonFactory = new com.google.api.client.json.jackson2.JacksonFactory();

            // Initialize Google Drive API client
            GoogleCredential credential = new GoogleCredential().setAccessToken(accessToken);
            driveService = new Drive.Builder(httpTransport, jsonFactory, credential)
                    .setApplicationName("KeyPic")
                    .build();

            File imageFile = files[0];

            // Compress the photo
            File compressedPhotoFile = compressPhoto(imageFile);

            if (compressedPhotoFile == null) {
                return false; // Compression failed, return false
            }

            try {
                String folderId = getFolderIdByName(folderName);

                if (folderId == null) {
                    return false;
                }

                // Check if a file with the same name already exists in the folder
                String existingFileId = getFileIdByName(fileName, folderId);
                if (existingFileId != null) {
                    // File with the same name exists, delete it
                    deleteFileById(existingFileId);
                }

                // Create a new file metadata
                com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
                fileMetadata.setName(fileName);
                fileMetadata.setParents(Collections.singletonList(folderId));
                FileContent fileContent = new FileContent("image/jpeg", compressedPhotoFile);

                // Upload the file to Google Drive
                com.google.api.services.drive.model.File uploadedFile = driveService.files().create(fileMetadata, fileContent)
                        .setFields("id, name")
                        .execute();

                // Print the uploaded file details
                System.out.println("File uploaded: " + uploadedFile.getName() + " (ID: " + uploadedFile.getId() + ")");
                deletePhoto(compressedPhotoFile);

                return true;

            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);

            progressDialog.dismiss();
            capture.setVisibility(View.VISIBLE);
            closeBtn.setVisibility(View.VISIBLE);

            if (success) {
                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
                finish();
            } else {
                Toast.makeText(CameraLaunchActivity.this, "Error: Network connection not found. Photo not saved.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private String getFolderIdByName(String folderName) throws IOException {
        FileList result = driveService.files().list()
                .setQ("mimeType='application/vnd.google-apps.folder' and name='" + folderName + "'")
                .setSpaces("drive")
                .setFields("files(id)")
                .execute();

        List<com.google.api.services.drive.model.File> files = result.getFiles();
        if (files != null && !files.isEmpty()) {
            return files.get(0).getId();
        } else {
            return null;
        }
    }

    private String getFileIdByName(String fileName, String folderId) throws IOException {
        FileList result = driveService.files().list()
                .setQ("mimeType!='application/vnd.google-apps.folder' and name='" + fileName + "' and '" + folderId + "' in parents")
                .setSpaces("drive")
                .setFields("files(id)")
                .execute();

        List<com.google.api.services.drive.model.File> files = result.getFiles();
        if (files != null && !files.isEmpty()) {
            return files.get(0).getId();
        } else {
            return null;
        }
    }

    private void deleteFileById(String fileId) throws IOException {
        driveService.files().delete(fileId).execute();
    }

    private File compressPhoto(File photoFile) {
        try {
            // Read the orientation from EXIF metadata
            ExifInterface exifInterface = new ExifInterface(photoFile.getAbsolutePath());
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            // Decode the bitmap with the desired orientation
            Bitmap bitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
            bitmap = rotateBitmap(bitmap, orientation);

            // Compress the bitmap as a JPEG file with a desired quality (e.g., 80% quality)
            int quality = 60; // Set your desired quality here
            FileOutputStream fos = new FileOutputStream(photoFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, fos);
            fos.close();

            return photoFile;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Bitmap rotateBitmap(Bitmap bitmap, int orientation) {
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
            default:
                // No rotation needed
                return bitmap;
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }


    /***********************************************************
     * Camera Options
     **********************************************************/
    private void setFlashIcon(Camera camera) {
        if (camera.getCameraInfo().hasFlashUnit()) {
            if (camera.getCameraInfo().getTorchState().getValue() == 0) {
                camera.getCameraControl().enableTorch(true);
                toggleFlash.setImageResource(R.drawable.baseline_flash_off_24);
            } else {
                camera.getCameraControl().enableTorch(false);
                toggleFlash.setImageResource(R.drawable.baseline_flash_on_24);
            }
        }
    }
}

