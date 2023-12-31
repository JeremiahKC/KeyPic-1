package com.kcassets.keypic;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CustomActivity extends AppCompatActivity {


    /***********************************************************
     * Initializations
     **********************************************************/
    EditText job, picFile;
    ImageView check;
    ImageView unitCheck;
    Button folderBtn, unitFolderBtn, minusBtn, plusBtn, photoBtn;
    Spinner unitSpinner;
    ImageView backArrow;
    private static final int CAMERA_REQUEST_CODE = 123;
    private Drive driveService;
    ProgressDialog progressDialog;
    TextView title, number, fileDisplay;
    ListView photoList;
    String folderName;
    String unitFolderName;
    String currentFolder;
    String unitNumber;
    String[] unitArray;
    String fileName;
    String accessToken;
    List<String> existingFileNames = new ArrayList<>();
    private int currentNumber = 1;
    String progressMessage;
    String toastMessage;
    SharedPreferences sharedPreferences;
    String PREF_FOLDER = "folder";


    /***********************************************************
     * onCreate
     **********************************************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.custom_photo_screen);


        /***********************************************************
         * Local Initializations
         **********************************************************/
        // Initialize spinners and list view and buttons
        folderBtn = findViewById(R.id.folderBtn);
        unitFolderBtn = findViewById(R.id.unitFolderBtn);
        unitSpinner = findViewById(R.id.unitFolderSpinner);
        minusBtn = findViewById(R.id.minusBtn);
        plusBtn = findViewById(R.id.plusBtn);
        photoBtn = findViewById(R.id.photoBtn);
        job = findViewById(R.id.job);
        picFile = findViewById(R.id.pic);
        check = findViewById(R.id.check);
        unitCheck = findViewById(R.id.checkunit);
        title = findViewById(R.id.title);
        number = findViewById(R.id.number);
        fileDisplay = findViewById(R.id.fileDisplay);
        backArrow = findViewById(R.id.backArrow);
        photoList = findViewById(R.id.photoList);

        progressDialog = new ProgressDialog(CustomActivity.this);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);

        check.setVisibility(View.INVISIBLE);
        unitCheck.setVisibility(View.INVISIBLE);
        number.setText(String.valueOf(currentNumber));

        fileName = picFile.getText().toString() + "_" + currentNumber + ".jpg";
        fileDisplay.setText(fileName);

        // Retrieve access token
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        folderName = sharedPreferences.getString("folder", null);

        AccessTokenManager accessTokenManager = new AccessTokenManager(this);
        accessTokenManager.checkAccessTokenExpiration();

        if (folderName != null) {

            job.setText(folderName);

            // Perform the Google Drive API task
            progressMessage = "Searching for folder... ";
            toastMessage = "Folder found in 'My Drive'";
            DriveTask driveTask = new DriveTask(folderName, null,progressDialog, progressMessage, toastMessage);
            driveTask.execute();
        }

        unitArray = getResources().getStringArray(R.array.custom_unit);

        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, unitArray);
        unitSpinner.setAdapter(unitAdapter);

        unitSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                unitCheck.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // Do nothing
            }
        });


        /***********************************************************
         * Buttons
         **********************************************************/
        photoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AccessTokenManager accessTokenManager = new AccessTokenManager(CustomActivity.this);
                accessTokenManager.checkAccessTokenExpiration();

                if (folderName != null && fileName != null) {

                    if (unitCheck.getVisibility() == View.VISIBLE) {
                        currentFolder = unitFolderName;
                    } else {
                        currentFolder = folderName;
                    }

                    if (existingFileNames.contains(fileName)){
                        showRetakeDialog();
                    } else {
                        String selectedPhoto = "Photo: " + fileName;
                        // Call the launchCamera method or perform desired action
                        launchCamera(selectedPhoto, fileName, currentFolder);
                    }
                } else {
                    Toast.makeText(CustomActivity.this, "Error: Job Folder or File Name is empty or invalid", Toast.LENGTH_SHORT).show();
                }
            }
        });

        backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CustomActivity.this, WelcomeActivity.class);
                startActivity(intent);
                finish();
            }
        });
        title.setText("Custom");

        // Get reference to the parent layout of your activity
        RelativeLayout parentLayout = findViewById(R.id.customLayout);

        // Set an OnTouchListener for the parent layout
        parentLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Close the keyboard
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(job.getWindowToken(), 0);

                // Remove focus and cursor from the EditText
                job.clearFocus();
                picFile.clearFocus();

                // Return false to allow other touch events to be processed
                return false;
            }
        });

        job.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Set the visibility of 'check' to GONE
                check.setVisibility(View.GONE);
                return false;
            }
        });

        // Add a TextWatcher to picFile EditText
        picFile.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Do nothing
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Update fileName and fileDisplay whenever the text in picFile changes
                fileName = charSequence.toString() + "_" + currentNumber + ".jpg";
                fileDisplay.setText(fileName);
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // Do nothing
            }
        });

        plusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                picFile.clearFocus();
                job.clearFocus();
                currentNumber++;
                number.setText(String.valueOf(currentNumber));
                fileName = picFile.getText().toString() + "_" + currentNumber + ".jpg";
                fileDisplay.setText(fileName);
            }
        });

        minusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentNumber > 1) {
                    picFile.clearFocus();
                    job.clearFocus();
                    currentNumber--;
                    number.setText(String.valueOf(currentNumber));
                    fileName = picFile.getText().toString() + "_" + currentNumber + ".jpg";
                    fileDisplay.setText(fileName);
                }
            }
        });

        // Set click listener for the search/create button
        folderBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AccessTokenManager accessTokenManager = new AccessTokenManager(CustomActivity.this);
                accessTokenManager.checkAccessTokenExpiration();

                unitCheck.setVisibility(View.INVISIBLE);

                // Get the folder name from the EditText
                folderName = job.getText().toString();
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(PREF_FOLDER, folderName);
                editor.apply();

                // Close the keyboard
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(job.getWindowToken(), 0);

                // Remove focus and cursor from the EditText
                job.clearFocus();

                // Perform the Google Drive API task
                progressMessage = "Searching for folder... ";
                toastMessage = "Folder found in 'My Drive'";
                DriveTask driveTask = new DriveTask(folderName, null, progressDialog, progressMessage, toastMessage);
                driveTask.execute();
            }
        });

        unitFolderBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AccessTokenManager accessTokenManager = new AccessTokenManager(CustomActivity.this);
                accessTokenManager.checkAccessTokenExpiration();

                if (folderName != null) {
                    unitNumber = unitSpinner.getSelectedItem().toString();

                    unitFolderName = folderName + "_Unit" + unitNumber;

                    progressMessage = "Searching for unit folder... ";
                    toastMessage = "Unit folder found in job folder";
                    DriveTask unitTask = new DriveTask(unitFolderName, folderName, progressDialog, progressMessage, toastMessage);
                    unitTask.execute();
                } else {
                    Toast.makeText(CustomActivity.this, "Error: No Job Folder Selected", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void showRetakeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(CustomActivity.this);
        builder.setTitle("Retake Photo");
        builder.setMessage("This photo already exists in the folder. Would you like to retake it?");
        builder.setPositiveButton("Retake", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String selectedPhoto = "Photo: " + fileName;
                // Call the launchCamera method or perform desired action
                launchCamera(selectedPhoto, fileName, folderName);
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }


    /***********************************************************
     * Camera Activity Launcher
     **********************************************************/
    private void launchCamera(String photoName, String fileName, String folderName) {
        Intent intent = new Intent(this, CameraLaunchActivity.class);
        intent.putExtra("photoName", photoName);
        intent.putExtra("fileName", fileName);
        intent.putExtra("folderName", folderName);
        startActivityForResult(intent, CAMERA_REQUEST_CODE);
    }


    /***********************************************************
     * Resume AmazonActivity and Update Highlight
     **********************************************************/
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            progressMessage = "Updating Folder... ";
            toastMessage = "Photo Saved Successfully";
            DriveTask updateList = new DriveTask(currentFolder, null,progressDialog, progressMessage, toastMessage);
            updateList.execute();
        }
    }


    /***********************************************************
     * Folder Check and Creation
     **********************************************************/
    public class DriveTask extends AsyncTask<String, Void, Boolean> {
        private String folderInput;
        private ProgressDialog progressDialog;
        private String parentFolderName;
        private boolean createFolder;
        private String progressMessage;
        private String toastMessage;

        public DriveTask(String folderInput, String parentFolderName, ProgressDialog progressDialog, String progressMessage, String toastMessage) {
            this.folderInput = folderInput;
            this.parentFolderName = parentFolderName;
            this.progressMessage = progressMessage;
            this.toastMessage = toastMessage;
            this.progressDialog = progressDialog;
            this.createFolder = false;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.setMessage(progressMessage);
            progressDialog.show();
        }

        @Override
        protected Boolean doInBackground(String... params) {
            try {
                sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
                accessToken = sharedPreferences.getString("access_token", null);

                HttpTransport httpTransport = new com.google.api.client.http.javanet.NetHttpTransport();
                JsonFactory jsonFactory = new com.google.api.client.json.jackson2.JacksonFactory();

                // Initialize Google Drive API client
                GoogleCredential credential = new GoogleCredential().setAccessToken(accessToken);
                driveService = new Drive.Builder(httpTransport, jsonFactory, credential)
                        .setApplicationName("KeyPic")
                        .build();

                // Search for the folder by name
                String query;
                if (parentFolderName != null) {
                    query = "mimeType='application/vnd.google-apps.folder' and name='" + folderInput + "' and '" + getParentFolderId(parentFolderName) + "' in parents";
                } else {
                    query = "mimeType='application/vnd.google-apps.folder' and name='" + folderInput + "'";
                }

                FileList result = driveService.files().list().setQ(query).setSpaces("drive").execute();
                List<File> files = result.getFiles();

                // Check if the folder exists
                if (files != null && !files.isEmpty()) {
                    // Folder exists
                    File folder = files.get(0);

                    // Gather file names that exist in that folder and add them to a list
                    String folderId = folder.getId();
                    String fileQuery = "'" + folderId + "' in parents and trashed=false";
                    FileList fileList = driveService.files().list().setQ(fileQuery).setSpaces("drive").execute();
                    List<File> folderFiles = fileList.getFiles();
                    existingFileNames.clear();
                    for (File file : folderFiles) {
                        existingFileNames.add(file.getName());
                    }
                } else {
                    // Folder does not exist
                    createFolder = true;
                }
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            progressDialog.dismiss();
            if (result) {
                if (!createFolder) {

                    if (parentFolderName != null) {
                        unitCheck.setVisibility(View.VISIBLE);
                    } else {
                        check.setVisibility(View.VISIBLE);
                    }

                    // Get the current list of items from the photoList ListView

                    // Add the fileName string at the first position of the list
                    List<String> currentList = new ArrayList<>(existingFileNames);

                    // Create or update the adapter with the new list of items
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(CustomActivity.this,android.R.layout.simple_list_item_1, currentList);

                    // Set the updated adapter to the photoList ListView
                    photoList.setAdapter(adapter);

                    Toast.makeText(CustomActivity.this, toastMessage, Toast.LENGTH_SHORT).show();
                }

                if (createFolder) {
                    showCreateFolderDialog();
                }
            } else {
                Toast.makeText(CustomActivity.this, "Error: Network connection not found.", Toast.LENGTH_LONG).show();
            }
        }

        private void showCreateFolderDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(progressDialog.getContext());
            builder.setMessage("This folder does not exist. Would you like to create it?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            if (parentFolderName != null) {
                                createFolderInsideParent();
                            } else {
                                createFolder();
                            }
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            if (parentFolderName != null) {
                                unitFolderName = null;
                            } else {
                                folderName = null;
                            }
                        }
                    });

            AlertDialog alert = builder.create();
            alert.show();
        }

        private String getParentFolderId(String parentFolderName) {
            try {
                String query = "mimeType='application/vnd.google-apps.folder' and name='" + parentFolderName + "'";
                FileList result = driveService.files().list().setQ(query).setSpaces("drive").execute();
                List<File> files = result.getFiles();

                if (files != null && !files.isEmpty()) {
                    return files.get(0).getId();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }


        /***********************************************************
         * Create Folder if it Does not Exist
         **********************************************************/
        private void createFolderInsideParent() {
            ProgressDialog createFolderProgressDialog = new ProgressDialog(CustomActivity.this);
            createFolderProgressDialog.setCancelable(false);

            new CreateFolderTask(folderInput, parentFolderName, createFolderProgressDialog).execute();
        }

        private void createFolder() {
            ProgressDialog createFolderProgressDialog = new ProgressDialog(CustomActivity.this);
            createFolderProgressDialog.setCancelable(false);

            new CreateFolderTask(folderInput, null, createFolderProgressDialog).execute();
        }

        private class CreateFolderTask extends AsyncTask<Void, Void, Boolean> {
            private String folderInput;
            private String parentFolderName;  // New field for the parent folder name
            private ProgressDialog progressDialog;

            public CreateFolderTask(String folderInput, String parentFolderName, ProgressDialog progressDialog) {
                this.folderInput = folderInput;
                this.parentFolderName = parentFolderName;
                this.progressDialog = progressDialog;
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                progressDialog.setMessage("Creating folder...");
                progressDialog.show();
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
                    accessToken = sharedPreferences.getString("access_token", null);

                    HttpTransport httpTransport = new com.google.api.client.http.javanet.NetHttpTransport();
                    JsonFactory jsonFactory = new com.google.api.client.json.jackson2.JacksonFactory();

                    // Initialize Google Drive API client
                    GoogleCredential credential = new GoogleCredential().setAccessToken(accessToken);
                    driveService = new Drive.Builder(httpTransport, jsonFactory, credential)
                            .setApplicationName("KeyPic")
                            .build();

                    File folderMetadata = new File();
                    folderMetadata.setName(folderInput);
                    folderMetadata.setMimeType("application/vnd.google-apps.folder");

                    if (parentFolderName != null) {
                        String parentFolderId = getParentFolderId(parentFolderName);
                        if (parentFolderId != null) {
                            folderMetadata.setParents(Collections.singletonList(parentFolderId));
                        }
                    }

                    File folder = driveService.files().create(folderMetadata).setFields("id").execute();
                    System.out.println("Folder created: " + folder.getName() + " (ID: " + folder.getId() + ")");
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);
                progressDialog.dismiss();

                if (result) {
                    existingFileNames.clear();
                    if (parentFolderName != null) {
                        unitCheck.setVisibility(View.VISIBLE);
                    } else {
                        check.setVisibility(View.VISIBLE);
                    }
                    Toast.makeText(CustomActivity.this, "Folder Created Successfully", Toast.LENGTH_SHORT).show();

                } else {
                    Toast.makeText(CustomActivity.this, "Error: Network connection not found.", Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}
