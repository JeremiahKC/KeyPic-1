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
import java.util.List;

public class AmazonActivity extends AppCompatActivity {


    /***********************************************************
     * Initializations
     **********************************************************/
    Spinner typeSpinner;
    Spinner phaseSpinner;
    Spinner unitSpinner;
    ListView photoList;
    EditText job;
    ImageView check;
    Button folderBtn;
    ImageView backArrow;
    private static final int CAMERA_REQUEST_CODE = 123;
    private Drive driveService;
    ProgressDialog progressDialog;
    TextView title;
    PhotoAdapter photoAdapter;
    String[] typeArray;
    String[] phaseArray;
    String[] unitArray;
    private String selectedType;
    private String selectedPhase;
    private String selectedUnit;
    String folderName;
    String progressMessage;
    String toastMessage;
    String accessToken;
    List<String> fileNames = new ArrayList<>();
    List<String> existingFileNames = new ArrayList<>();
    SharedPreferences sharedPreferences;
    String PREF_FOLDER = "folder";
    String PREF_TYPE = "type";
    String PREF_PHASE = "phase";
    String PREF_UNIT = "unit";
    String PREF_TYPE_POS = "type_pos";
    String PREF_PHASE_POS = "phase_pos";
    String PREF_UNIT_POS = "unit_pos";


    /***********************************************************
     * onCreate
     **********************************************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.photo_screen);


        /***********************************************************
         * Local Initializations
         **********************************************************/
        // Initialize elements
        typeSpinner = findViewById(R.id.typeSpinner);
        phaseSpinner = findViewById(R.id.phaseSpinner);
        unitSpinner = findViewById(R.id.unitSpinner);
        photoList = findViewById(R.id.photoList);
        folderBtn = findViewById(R.id.folderBtn);
        job = findViewById(R.id.job);
        check = findViewById(R.id.check);
        title = findViewById(R.id.title);
        backArrow = findViewById(R.id.backArrow);

        // Loading Popup Initialization
        progressDialog = new ProgressDialog(AmazonActivity.this);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);

        check.setVisibility(View.INVISIBLE);

        // Initialize arrays from resources
        typeArray = getResources().getStringArray(R.array.amazon_type);
        phaseArray = getResources().getStringArray(R.array.amazon_phase);
        unitArray = getResources().getStringArray(R.array.amazon_unit);

        // Setup Spinners
        setupSpinners();

        // Retrieve Previous Folder and Spinner Selections
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        folderName = sharedPreferences.getString("folder", null);
        selectedType = sharedPreferences.getString("type", null);
        selectedPhase = sharedPreferences.getString("phase", null);
        selectedUnit = sharedPreferences.getString("unit", null);
        int typePosition = sharedPreferences.getInt("type_pos", -1);
        int phasePosition = sharedPreferences.getInt("phase_pos", -1);
        int unitPosition = sharedPreferences.getInt("unit_pos", -1);

        // Check expiration time of access token
        AccessTokenManager accessTokenManager = new AccessTokenManager(this);
        accessTokenManager.checkAccessTokenExpiration();

        if (selectedType != null && selectedPhase != null && selectedUnit != null) {

            typeSpinner.setSelection(typePosition);
            phaseSpinner.setSelection(phasePosition);
            unitSpinner.setSelection(unitPosition);
        }

        if (folderName != null) {

            job.setText(folderName);

            // Perform the Google Drive API task
            progressMessage = "Searching for folder... ";
            toastMessage = "Folder found in 'My Drive'";
            DriveTask driveTask = new DriveTask(folderName, progressDialog, progressMessage, toastMessage);
            driveTask.execute();
        }


        /***********************************************************
         * Buttons
         **********************************************************/
        backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AmazonActivity.this, WelcomeActivity.class);
                startActivity(intent);
                finish();
            }
        });
        title.setText("Package");

        // Set click listener for photo list items
        photoList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AccessTokenManager accessTokenManager = new AccessTokenManager(AmazonActivity.this);
                accessTokenManager.checkAccessTokenExpiration();

                if (folderName != null) {
                    // Check if the item has a green background
                    boolean isGreenBackground = isGreenBackground(view);

                    if (isGreenBackground) {
                        // Show a dialog asking the user if they want to retake the photo
                        showRetakeDialog(position);
                    } else {
                        // Get the selected photo name
                        String selectedPhoto = (String) parent.getItemAtPosition(position);
                        String selectedFileName = fileNames.get(position);
                        // Call the launchCamera method or perform desired action
                        launchCamera(selectedPhoto, selectedFileName, folderName);
                    }
                } else {
                    Toast.makeText(AmazonActivity.this, "Error: Job Number/Folder is empty or invalid", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Get reference to the parent layout of your activity
        RelativeLayout parentLayout = findViewById(R.id.photoLayout);

        // Set an OnTouchListener for the parent layout
        parentLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Close the keyboard
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(job.getWindowToken(), 0);

                // Remove focus and cursor from the EditText
                job.clearFocus();

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

        // Set click listener for the search/create button
        folderBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check expiration time of access token
                AccessTokenManager accessTokenManager = new AccessTokenManager(AmazonActivity.this);
                accessTokenManager.checkAccessTokenExpiration();

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
                DriveTask driveTask = new DriveTask(folderName, progressDialog, progressMessage, toastMessage);
                driveTask.execute();
            }
        });
    }


    /***********************************************************
     * Photo Retake Handling
     **********************************************************/
    private boolean isGreenBackground(View view) {
        Drawable background = view.getBackground();
        if (background instanceof ColorDrawable) {
            int backgroundColor = ((ColorDrawable) background).getColor();
            return backgroundColor == ContextCompat.getColor(AmazonActivity.this, R.color.photoColor);
        }
        return false;
    }

    private void showRetakeDialog(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(AmazonActivity.this);
        builder.setTitle("Retake Photo");
        builder.setMessage("This photo already exists in the folder. Would you like to retake it?");
        builder.setPositiveButton("Retake", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Get the selected photo name
                String selectedPhoto = (String) photoList.getItemAtPosition(position);
                String selectedFileName = fileNames.get(position);
                // Call the launchCamera method or perform desired action
                launchCamera(selectedPhoto, selectedFileName, folderName);
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }


    /***********************************************************
     * Spinner Setup
     **********************************************************/
    private void setupSpinners() {
        // Set adapters for spinners
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, typeArray);
        typeSpinner.setAdapter(typeAdapter);

        ArrayAdapter<String> phaseAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, phaseArray);
        phaseSpinner.setAdapter(phaseAdapter);

        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, unitArray);
        unitSpinner.setAdapter(unitAdapter);

        typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                makePhotoList();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // Do nothing
            }
        });

        phaseSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                makePhotoList();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // Do nothing
            }
        });

        unitSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                makePhotoList();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // Do nothing
            }
        });
    }


    /***********************************************************
     * Photo List Creation
     **********************************************************/
    private void makePhotoList() {
        // Get selected spinner values
        selectedType = typeSpinner.getSelectedItem().toString();
        selectedPhase = phaseSpinner.getSelectedItem().toString();
        selectedUnit = unitSpinner.getSelectedItem().toString();

        int typePosition = typeSpinner.getSelectedItemPosition();
        int phasePosition = phaseSpinner.getSelectedItemPosition();
        int unitPosition = unitSpinner.getSelectedItemPosition();

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PREF_TYPE, selectedType);
        editor.putString(PREF_PHASE, selectedPhase);
        editor.putString(PREF_UNIT, selectedUnit);
        editor.putInt(PREF_TYPE_POS, typePosition);
        editor.putInt(PREF_PHASE_POS, phasePosition);
        editor.putInt(PREF_UNIT_POS, unitPosition);
        editor.apply();

        // Create an array name based on the selected values
        String arrayName = selectedType + "_" + selectedPhase + "_" + selectedUnit;

        // Get the resource ID of the array dynamically
        int arrayId = getResources().getIdentifier(arrayName, "array", getPackageName());

        // Populate the list view with the selected array
        String[] selectedArray = new String[0];
        if (arrayId != 0) {
            selectedArray = getResources().getStringArray(arrayId);
        }

        fileNames.clear();

        // Get the resource ID of the file name array dynamically
        int fileNameArrayId = getResources().getIdentifier(arrayName + "_File", "array", getPackageName());

        // Retrieve the string array of file names using the resource ID
        String[] fileNameArray = getResources().getStringArray(fileNameArrayId);

        // Match each item in the ListView array with the corresponding file name using their positions in the arrays
        List<String> listViewItems = Arrays.asList(selectedArray);

        for (int i = 0; i < listViewItems.size(); i++) {
            String listViewItem = listViewItems.get(i);
            String fileName = fileNameArray[i];
            fileNames.add(fileName);
        }

        photoAdapter = new PhotoAdapter(getApplicationContext(), fileNames, existingFileNames, listViewItems);
        photoList.setAdapter(photoAdapter);
    }


    /***********************************************************
     * Camera Activity Launcher
     **********************************************************/
    private void launchCamera(String photoName, String fileName, String folderName) {
        Intent intent = new Intent(this, CameraLaunchActivity.class);
        intent.putExtra("photoName", photoName);
        intent.putExtra("fileName", fileName);
        intent.putExtra("folderName", folderName);
        intent.putExtra("selectedType", selectedType);
        intent.putExtra("selectedPhase", selectedPhase);
        intent.putExtra("selectedUnit", selectedUnit);
        startActivityForResult(intent, CAMERA_REQUEST_CODE);
    }


    /***********************************************************
     * Resume AmazonActivity and Update Highlight
     **********************************************************/
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST_CODE && (resultCode == RESULT_OK || resultCode == RESULT_CANCELED)) {
            // Restore the state of the activity
            if (data != null) {
                selectedType = data.getStringExtra("selectedType");
                selectedPhase = data.getStringExtra("selectedPhase");
                selectedUnit = data.getStringExtra("selectedUnit");

                // Set the spinner values
                typeSpinner.setSelection(getIndex(typeArray, selectedType));
                phaseSpinner.setSelection(getIndex(phaseArray, selectedPhase));
                unitSpinner.setSelection(getIndex(unitArray, selectedUnit));
            }
        }
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            progressMessage = "Updating List... ";
            toastMessage = "Photo Saved Successfully";
            DriveTask updateList = new DriveTask(folderName, progressDialog, progressMessage, toastMessage);
            updateList.execute();
        }
    }

    private int getIndex(String[] array, String value) {
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(value)) {
                return i;
            }
        }
        return -1;
    }


    /***********************************************************
     * Folder Check and Creation
     **********************************************************/
    public class DriveTask extends AsyncTask<String, Void, Boolean> {
        private String folderInput;
        private ProgressDialog progressDialog;
        private boolean createFolder;
        private String progressMessage;
        private String toastMessage;

        public DriveTask(String folderInput, ProgressDialog progressDialog, String progressMessage, String toastMessage) {
            this.folderInput = folderInput;
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
                String query = "mimeType='application/vnd.google-apps.folder' and name='" + folderInput + "'";
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
                    check.setVisibility(View.VISIBLE);
                    Toast.makeText(AmazonActivity.this, toastMessage, Toast.LENGTH_SHORT).show();
                    if (!fileNames.isEmpty()) {
                        makePhotoList();
                    }
                }

                if (createFolder) {
                    showCreateFolderDialog();
                }
            } else {
                Toast.makeText(AmazonActivity.this, "Error: Network connection not found.", Toast.LENGTH_LONG).show();
            }
        }

        private void showCreateFolderDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(progressDialog.getContext());
            builder.setMessage("This folder does not exist. Would you like to create it?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            createFolder();
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            folderName = null;
                        }
                    });

            AlertDialog alert = builder.create();
            alert.show();
        }


        /***********************************************************
         * Create Folder if it Does not Exist
         **********************************************************/
        private void createFolder() {
            ProgressDialog createFolderProgressDialog = new ProgressDialog(AmazonActivity.this);
            createFolderProgressDialog.setCancelable(false);

            new CreateFolderTask(folderInput, createFolderProgressDialog).execute();
        }

        private class CreateFolderTask extends AsyncTask<Void, Void, Boolean> {
            private String folderInput;
            private ProgressDialog progressDialog;

            public CreateFolderTask(String folderInput, ProgressDialog progressDialog) {
                this.folderInput = folderInput;
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
                    /***********************************************************
                     * Google Drive Folder Check
                     **********************************************************/
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
                    if (photoAdapter != null) {
                        existingFileNames.clear();
                        List<String> emptyList = new ArrayList<>(); // Create an empty list
                        photoAdapter = new PhotoAdapter(getApplicationContext(), fileNames, existingFileNames, emptyList);
                        photoList.setAdapter(photoAdapter);
                    }
                    existingFileNames.clear();
                    makePhotoList();
                    check.setVisibility(View.VISIBLE);
                    Toast.makeText(AmazonActivity.this, "Folder Created in 'My Drive'", Toast.LENGTH_SHORT).show();

                } else {
                    Toast.makeText(AmazonActivity.this, "Error: Network connection not found.", Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}
