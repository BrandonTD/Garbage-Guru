package td.brandon.garbageguru;

import android.Manifest;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import td.brandon.garbageguru.utility.PermissionUtils;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link CurrentImageFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link CurrentImageFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CurrentImageFragment extends Fragment {
    @BindView(R.id.objectImage) ImageView objectImage;
    @BindView(R.id.bluebinimage) ImageView bluebinImage;
    @BindView(R.id.greenbinimage) ImageView greenbinImage;
    @BindView(R.id.greybinimage) ImageView greybinImage;
    @BindView(R.id.ewasteimage) ImageView ewasteImage;
    @BindView(R.id.transferstationimage) ImageView transferstationImage;
    @BindView(R.id.scrapmetalimage) ImageView scrapmetalImage;
    @BindView(R.id.prohibitedimage) ImageView prohibitedwasteImage;
    @BindView(R.id.hhwimage) ImageView householdwasteImage;
    @BindView(R.id.yardwasteimage) ImageView yardwasteImage;
    @BindView(R.id.objectName) TextView objectName;
    @BindView(R.id.noDisposalText) TextView noDisposalText;

    private File photoFile = null;
    private OkHttpClient client = new OkHttpClient();

    private String apiKey;
    private String dataEndpoint = "https://vision.googleapis.com/v1/images:annotate?key=";
    private String databaseEndpoint = "https://856544a4.ngrok.io/api/disposalmethods/";

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    public static final int CAMERA_PERMISSIONS_REQUEST = 0;
    public static final int CAMERA_IMAGE_REQUEST = 1;

    private OnFragmentInteractionListener mListener;

    // Required empty public constructor
    public CurrentImageFragment() {
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment CurrentImageFragment.
     */
    public static CurrentImageFragment newInstance() {
        CurrentImageFragment fragment = new CurrentImageFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_current_image, container, false);

        ButterKnife.bind(this, view);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        apiKey = getString(R.string.api_key);
        dataEndpoint +=  apiKey;

        Log.d("api", apiKey);

        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
            Log.d("status", "creating file");
            // Create the File where the photo should go
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }

            // Continue only if the File was successfully created
            if (photoFile != null) {
                Log.d("status", "file created");
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                startCamera();
            }
        }

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }


    public void startCamera() {
        if (PermissionUtils.requestPermission(
                getActivity(),
                CAMERA_PERMISSIONS_REQUEST,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA)) {
            Log.d("status", "starting camera");
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            Uri photoUri = FileProvider.getUriForFile(getActivity(), getActivity().getApplicationContext().getPackageName() + ".provider", photoFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, CAMERA_IMAGE_REQUEST);
        }
    }

    /**
     * This method displays the camera image on the screen and sends the image to the camFind API for analysis
     * @param requestCode
     * @param resultCode
     * @param data
     * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        String photoPath = photoFile.getAbsolutePath();

        Matrix matrix = new Matrix();
        matrix.postRotate(90);

        //Convert photo file into a bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        Bitmap bitmap = BitmapFactory.decodeFile(photoPath, bmOptions);
        bitmap = scaleBitmapDown(bitmap, 900);

        //Flip bitmap right side up and display on screen
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        objectImage.setImageBitmap(bitmap);

        //encode image to base64 string
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageBytes = baos.toByteArray();
        String imageString = Base64.encodeToString(imageBytes, Base64.DEFAULT);

        //calls for the response from vision api
        new callVisionAPI().execute(dataEndpoint, imageString);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d("status", "request permission");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CAMERA_PERMISSIONS_REQUEST:
                Log.d("status", "camera permission request");
                if (PermissionUtils.permissionGranted(requestCode, CAMERA_PERMISSIONS_REQUEST, grantResults)) {
                    Log.d("status", "camera permission granted");
                    startCamera();
                }
                break;
        }
    }

    /**
     * Creates the image file and stores it in the app's private directory
     * @return image file
     * @throws IOException
     */
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";

        //Directory of the image file
        File dir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        File image = new File(dir, imageFileName);

        return image;
    }

    public Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {

        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;

        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }

    private String runVisionRequest(String url, String imageString) throws IOException {
        RequestBody body = RequestBody.create(JSON, visionJsonBody(imageString));
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    String visionJsonBody(String imageString) {
        return "{'requests':[{"
                + "'image':{ 'content':'" + imageString + "'},"
                + "'features':["
                + "{'type': 'LABEL_DETECTION', 'maxResults':1}]"
                + "}]}";
    }

    private class callVisionAPI extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String[] params) {
            try {
                String response = runVisionRequest(params[0], params[1]);

                Log.d("api", response);

                return response;
            } catch (Exception e) {
                Log.d("api", "Failed get request: " + e.toString());
            }

            return null;
        }

        /**
         * Method ran after retrieval of JSON response from api that is responsible for parsing .txt files to identify
         * the disposal method of the object and displaying the information on the screen
         *
         * @param response the api response that holds the name of the scanned object
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(String response) {
            JSONObject data;
            JSONObject responses;
            JSONArray detectedObjects;
            JSONObject highestScoreObject;
            String object;

            Log.d("api", response);

            if (response != (null)) {
                try {//parse JSON response to get the name of the object
                    data = new JSONObject(response);
                    responses = data.getJSONArray("responses").getJSONObject(0);
                    detectedObjects = responses.getJSONArray("labelAnnotations");
                    highestScoreObject = detectedObjects.getJSONObject(0);
                    object = highestScoreObject.getString("description");

                    Log.d("api", "object recognized: " + object);

                    objectName.setText(object);
                    getDisposalMethods(databaseEndpoint + object);
                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }


            } else {
                Log.d("api", "UNSUCCESSFUL REQUEST: no response");
            }
        }

        public void getDisposalMethods(final String url) {
            new AsyncTask<String, Void, String>() {
                protected String doInBackground(String... params) {
                    try {
                        String response = runDatabaseRequest(url);

                        Log.d("api", response);

                        return response;
                    } catch (Exception e) {
                        Log.d("api", "Failed get request: " + e.toString());
                    }

                    return null;
                }
                protected void onPostExecute(String response) {
                    JSONObject data;
                    JSONArray methods;

                    Log.d("api", response);

                    if (response != (null)) {
                        try {//parse JSON response to get the name of the object
                            data = new JSONObject(response);
                            methods = data.getJSONArray("data");
                            Log.d("api", "disposal methods: " + methods.toString());

                            if (methods.length() == 0) {
                                noDisposalText.setVisibility(View.VISIBLE);
                                return;
                            }

                            for (int i = 0; i < methods.length(); i++) {
                                String method = methods.getString(i);

                                switch(method) {
                                    case "bluebin" :
                                        bluebinImage.setVisibility(View.VISIBLE);
                                        break;
                                    case "greenbin" :
                                        greenbinImage.setVisibility(View.VISIBLE);
                                        break;
                                    case "greybin" :
                                        greybinImage.setVisibility(View.VISIBLE);
                                        break;
                                    case "ewaste" :
                                        ewasteImage.setVisibility(View.VISIBLE);
                                        break;
                                    case "transferstation" :
                                        transferstationImage.setVisibility(View.VISIBLE);
                                        break;
                                    case "householdwaste" :
                                        householdwasteImage.setVisibility(View.VISIBLE);
                                        break;
                                    case "yardwaste" :
                                        yardwasteImage.setVisibility(View.VISIBLE);
                                        break;
                                    case "prohibited" :
                                        prohibitedwasteImage.setVisibility(View.VISIBLE);
                                        break;
                                    case "scrapmetal" :
                                        scrapmetalImage.setVisibility(View.VISIBLE);
                                        break;
                                }
                            }
                        } catch (JSONException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }


                    } else {
                        Log.d("api", "UNSUCCESSFUL REQUEST: no response");
                    }
                }
                private String runDatabaseRequest(String url) throws IOException {
                    Request request = new Request.Builder()
                            .url(url)
                            .build();

                    Response response = client.newCall(request).execute();
                    return response.body().string();
                }
            }.execute();
        }
    }
}
