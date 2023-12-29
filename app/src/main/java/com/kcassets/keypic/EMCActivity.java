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
import android.view.ViewGroup;
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

public class EMCActivity extends AppCompatActivity {


    /***********************************************************
     * Initializations
     **********************************************************/
    Spinner typeSpinner;
    Spinner phaseSpinner;
    Spinner phaseSpinner2;
    Spinner unitSpinner;
    ListView photoList;
    EditText job;
    ImageView check;
    ImageView unitCheck;
    Button folderBtn, unitFolderBtn;
    ImageView backArrow;
    private static final int CAMERA_REQUEST_CODE = 123;
    private Drive driveService;
    ProgressDialog progressDialog;
    TextView title, textType, textPhase;
    PhotoAdapter photoAdapter;
    String[] categoryArray;
    String[] typeArray;
    String[] typeArray2;
    String[] unitArray;
    private String selectedType;
    private String selectedPhase;
    String folderName;
    String unitFolderName;
    String currentFolder;
    String unitNumber;
    String progressMessage;
    String toastMessage;
    String accessToken;
    List<String> fileNames = new ArrayList<>();
    List<String> existingFileNames = new ArrayList<>();
    SharedPreferences sharedPreferences;
    String PREF_FOLDER_EMC = "folder_EMC";
    String PREF_TYPE_EMC = "type_EMC";
    String PREF_UNIT_EMC = "unit_EMC";
    String PREF_TYPE_POS_EMC = "type_pos_EMC";
    String PREF_CURRENT_TYPE = "current_type";


    /***********************************************************
     * onCreate
     **********************************************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.photo_screen_emc);


        /***********************************************************
         * Local Initializations
         **********************************************************/
        // Initialize spinners and list view and buttons
        typeSpinner = findViewById(R.id.typeSpinner);
        phaseSpinner = findViewById(R.id.phaseSpinner);
        phaseSpinner2 = findViewById(R.id.phaseSpinner2);
        unitSpinner = findViewById(R.id.unitFolderSpinner);
        photoList = findViewById(R.id.photoList);
        folderBtn = findViewById(R.id.folderBtn);
        unitFolderBtn = findViewById(R.id.unitFolderBtn);
        job = findViewById(R.id.job);
        check = findViewById(R.id.check);
        unitCheck = findViewById(R.id.checkunit);
        title = findViewById(R.id.title);
        backArrow = findViewById(R.id.backArrow);
        textType = findViewById(R.id.textType);
        textPhase = findViewById(R.id.textPhase);

        progressDialog = new ProgressDialog(EMCActivity.this);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);

        // Hide/Move Elements
        check.setVisibility(View.INVISIBLE);
        unitCheck.setVisibility(View.INVISIBLE);
        textType.setText("Test Category");
        textPhase.setText("Test Type");

        // Initialize arrays from resources
        categoryArray = getResources().getStringArray(R.array.test_category);
        typeArray = getResources().getStringArray(R.array.test_type);
        typeArray2 = getResources().getStringArray(R.array.test_type2);

        // Setup Spinners
        setupSpinners();

        // Retrieve access token
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        folderName = sharedPreferences.getString("folder_EMC", null);
        selectedType = sharedPreferences.getString("type_EMC", null);
        int typePosition = sharedPreferences.getInt("type_pos_EMC", -1);
        int unitPosition = sharedPreferences.getInt("unit_pos_EMC", -1);
        int currentType = sharedPreferences.getInt("current_type", -1);

        AccessTokenManager accessTokenManager = new AccessTokenManager(this);
        accessTokenManager.checkAccessTokenExpiration();

    //    if (selectedType != null) {
    //        if (currentType == 0) {
    //            phaseSpinner.setVisibility(View.VISIBLE);
    //            phaseSpinner2.setVisibility(View.GONE);
    //        }
    //        if (currentType == 1) {
    //            phaseSpinner2.setVisibility(View.VISIBLE);
    //            phaseSpinner.setVisibility(View.GONE);
    //        }
    //
    //        typeSpinner.setSelection(typePosition);
    //        unitSpinner.setSelection(unitPosition);
    //    }

        if (folderName != null) {

            job.setText(folderName);

            // Perform the Google Drive API task
            progressMessage = "Searching for folder... ";
            toastMessage = "Folder found in 'My Drive'";
            DriveTask driveTask = new DriveTask(folderName,null, progressDialog, progressMessage, toastMessage);
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
        backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(EMCActivity.this, WelcomeActivity.class);
                startActivity(intent);
                finish();
            }
        });
        title.setText("EMC");

        // Set click listener for photo list items
        photoList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AccessTokenManager accessTokenManager = new AccessTokenManager(EMCActivity.this);
                accessTokenManager.checkAccessTokenExpiration();

                if (folderName != null && unitCheck.getVisibility() == View.VISIBLE) {
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
                        launchCamera(selectedPhoto, selectedFileName, unitFolderName);
                    }
                } else {
                    Toast.makeText(EMCActivity.this, "Error: Job Folder or Unit Folder is empty", Toast.LENGTH_SHORT).show();
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
                AccessTokenManager accessTokenManager = new AccessTokenManager(EMCActivity.this);
                accessTokenManager.checkAccessTokenExpiration();

                unitCheck.setVisibility(View.INVISIBLE);

                // Get the folder name from the EditText
                folderName = job.getText().toString();
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(PREF_FOLDER_EMC, folderName);
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
                AccessTokenManager accessTokenManager = new AccessTokenManager(EMCActivity.this);
                accessTokenManager.checkAccessTokenExpiration();

                if (folderName != null) {
                    unitNumber = unitSpinner.getSelectedItem().toString();

                    unitFolderName = folderName + "_Unit" + unitNumber;

                    progressMessage = "Searching for unit folder... ";
                    toastMessage = "Unit folder found in job folder";
                    DriveTask unitTask = new DriveTask(unitFolderName, folderName, progressDialog, progressMessage, toastMessage);
                    unitTask.execute();
                } else {
                    Toast.makeText(EMCActivity.this, "Error: No Job Folder Selected", Toast.LENGTH_LONG).show();
                }
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
            return backgroundColor == ContextCompat.getColor(EMCActivity.this, R.color.photoColor);
        }
        return false;
    }

    private void showRetakeDialog(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(EMCActivity.this);
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
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categoryArray);
        typeSpinner.setAdapter(categoryAdapter);

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, typeArray);
        phaseSpinner.setAdapter(typeAdapter);

        ArrayAdapter<String> typeAdapter2 = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, typeArray2);
        phaseSpinner2.setAdapter(typeAdapter2);

        typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                selectedPhase = typeSpinner.getSelectedItem().toString();
                if (selectedPhase.equals("CE MARK/FCC")) {
                    phaseSpinner2.setVisibility(View.GONE);
                    phaseSpinner.setVisibility(View.VISIBLE);
                }
                if (selectedPhase.equals("MIL-STD-461")) {
                    phaseSpinner.setVisibility(View.GONE);
                    phaseSpinner2.setVisibility(View.VISIBLE);
                }
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

        phaseSpinner2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
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
        int typePosition;
        int currentType;

        if (phaseSpinner.getVisibility() == View.VISIBLE) {
            currentType = 0;
            selectedType = phaseSpinner.getSelectedItem().toString();
            typePosition = phaseSpinner.getSelectedItemPosition();
        } else {
            selectedType = phaseSpinner2.getSelectedItem().toString();
            typePosition = phaseSpinner2.getSelectedItemPosition();
            currentType = 1;
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(PREF_CURRENT_TYPE, currentType);
        editor.putString(PREF_TYPE_EMC, selectedType);
        editor.putInt(PREF_TYPE_POS_EMC, typePosition);
        editor.apply();

        String arrayName;

        // Create an array name based on the selected values
        arrayName = selectedType;

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
                    Toast.makeText(EMCActivity.this, toastMessage, Toast.LENGTH_SHORT).show();
                    if (!fileNames.isEmpty()) {
                        makePhotoList();
                    }
                }

                if (createFolder) {
                    showCreateFolderDialog();
                }
            } else {
                Toast.makeText(EMCActivity.this, "Error: Network connection not found.", Toast.LENGTH_LONG).show();
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
            ProgressDialog createFolderProgressDialog = new ProgressDialog(EMCActivity.this);
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
                    Toast.makeText(EMCActivity.this, "Folder Created in 'My Drive'", Toast.LENGTH_SHORT).show();

                } else {
                    Toast.makeText(EMCActivity.this, "Error: Network connection not found.", Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}
