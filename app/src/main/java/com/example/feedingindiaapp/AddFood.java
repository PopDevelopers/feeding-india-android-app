package com.example.feedingindiaapp;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class AddFood extends AppCompatActivity implements  View.OnClickListener {
    protected int LOAD_IMAGE_CAMERA = 0, CROP_IMAGE = 1, LOAD_IMAGE_GALLARY = 2;
    ProgressDialog progressDialog;
    JSONObject jsonObject;
    JSONArray jsonArray;
    File pic;
    File croppedPic;
    Button seemap;
    Bundle extras;
    String foodPicUrl;
    private int donor_id ;
    private Uri picUri;
    private Uri croppedPicUri;
    private TextView latitude;
    private TextView longitude;
    private Spinner city;
    private EditText area;
    private EditText street;
    private EditText houseno;
    private String has_pickup_gps ="0";
    private String pickup_gps_latitude = "";
    private String pickup_gps_longitude = "";
    private ImageView upload_pic;
    private CheckBox isveg;
    private CheckBox isperishable;
    private EditText other_details;
    private FloatingActionButton submit_button;
    private String finalurl ;
    private String  password_hash;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_food);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.inflateMenu(R.menu.main);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setHomeButtonEnabled(true);
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        SharedPreferences sharedPreferences = getSharedPreferences("app_data", MODE_PRIVATE);
        donor_id =  sharedPreferences.getInt("user_id",0);
        password_hash = sharedPreferences.getString("user_password_hash", null);

        finalurl = donor_id+ String.valueOf(System.currentTimeMillis());
        latitude = (TextView) findViewById(R.id.latitude_map);
        longitude = (TextView) findViewById(R.id.longitude_map);
        latitude.setVisibility(View.GONE);
        longitude.setVisibility(View.GONE);
        seemap =(Button) findViewById(R.id.map_button);
        area = (EditText) findViewById(R.id.area);
        street = (EditText) findViewById(R.id.street);
        houseno = (EditText) findViewById(R.id.houseno);
        city = (Spinner) findViewById(R.id.spinner_city);
        upload_pic = (ImageView) findViewById(R.id.upload_pic);
        isveg = (CheckBox) findViewById(R.id.is_veg);
        isperishable = (CheckBox) findViewById(R.id.is_perishable);
        other_details = (EditText) findViewById(R.id.other_details);

        submit_button = (FloatingActionButton) findViewById(R.id.submit_button);
        submit_button.setOnClickListener(this);
        seemap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(AddFood.this, Getlocation.class);
                startActivityForResult(i,10);
            }
        });
        upload_pic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final CharSequence[] options = {"Take Photo", "Choose from Gallery"};

                AlertDialog.Builder builder = new AlertDialog.Builder(AddFood.this);
                builder.setTitle("Select Pic Using...");
                builder.setItems(options, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        if (options[item].equals("Take Photo")) {

                            try {

                                pic = new File(Environment.getExternalStorageDirectory(),
                                        "tmp_" + String.valueOf(System.currentTimeMillis()) + ".jpg");

                                picUri = Uri.fromFile(pic);

                                croppedPic = new File(Environment.getExternalStorageDirectory(),
                                        "tmp_" + String.valueOf(System.currentTimeMillis()) + ".jpg");

                                croppedPicUri = Uri.fromFile(pic); //change this

                                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                                cameraIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, picUri);

                                cameraIntent.putExtra("return-data", true);
                                startActivityForResult(cameraIntent, LOAD_IMAGE_CAMERA);
                            } catch (ActivityNotFoundException e) {
                                e.printStackTrace();
                            }

                        } else if (options[item].equals("Choose from Gallery")) {
                            Intent intent = new Intent(Intent.ACTION_PICK,
                                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                            startActivityForResult(Intent.createChooser(intent, "Select Picture"), LOAD_IMAGE_GALLARY);
                        }
                    }
                });

                builder.show();

            }
        });

    }



    public boolean validate() {
        boolean valid = true;

        String areaname = area.getText().toString();
        String house = houseno.getText().toString();
        String backgroundImageName = String.valueOf(upload_pic.getTag());
        if (areaname.isEmpty() || areaname.length() < 3) {
            area.setError("at least 3 characters");
            valid = false;
        } else {
            area.setError(null);
        }

        if (house.isEmpty() ) {
            houseno.setError("enter your house no.");
            valid = false;
        } else {
            houseno.setError(null);
        }



        return valid;
    }


    protected void CropImage() {
        try {
            Intent intent = new Intent("com.android.camera.action.CROP");
            intent.setDataAndType(picUri, "image/*");

            intent.putExtra("crop", "true");
            intent.putExtra("outputX", 200);
            intent.putExtra("outputY", 200);
            intent.putExtra("aspectX", 3);
            intent.putExtra("aspectY", 4);
            intent.putExtra("scaleUpIfNeeded", true);
            intent.putExtra("return-data", true);
//            pic = new File(Environment.getExternalStorageDirectory(),
//                    "tmp_" + String.valueOf(System.currentTimeMillis()) + ".jpg");
//
//            picUri = Uri.fromFile(pic);
            intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, croppedPicUri);

            startActivityForResult(intent, CROP_IMAGE);

        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Your device doesn't support the crop action!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(View v) {
        if(!validate())
        {
            return;

        }
        Resources resources = this.getResources();
        String[] codes = resources.getStringArray(R.array.city_arrays);
        int pos = city.getSelectedItemPosition();

        String city = codes[pos];
        String areaname = area.getText().toString();
        String streetname = street.getText().toString();
        String house = houseno.getText().toString();

        Boolean veg = isveg.isChecked();
        //true if veg
        Boolean perishable = isperishable.isChecked();
        //true if persihable
        String stringisveg="0",stringisperishable="0";
        if(veg==true)
            stringisveg="1";
        if(perishable==true)
            stringisperishable="1";



        String otherdetails = other_details.getText().toString();

        new uploadcloudinary().execute();

        System.out.println("B4 sending the url is: " + foodPicUrl);
        String[] details = {Integer.toString(donor_id), foodPicUrl, city, areaname, streetname, house, has_pickup_gps, pickup_gps_latitude, pickup_gps_longitude, stringisveg
                , stringisperishable, otherdetails};

        Bgtask bg = new Bgtask();
        bg.execute(details);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LOAD_IMAGE_CAMERA && resultCode == RESULT_OK) {
            CropImage();

        }
        else if (requestCode == LOAD_IMAGE_GALLARY) {
            if (data != null) {

                picUri = data.getData();
                CropImage();
            }
        }
        else if (requestCode == CROP_IMAGE) {
            if (data != null) {
                extras = data.getExtras();

                // get the cropped bitmap
                Bitmap photo = extras.getParcelable("data");
                upload_pic.setImageBitmap(photo);
                upload_pic.setTag("newpic");

                try {
                    FileOutputStream fos = new FileOutputStream(croppedPic);
                    photo.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                    fos.close();
                } catch (FileNotFoundException e) {
                    System.out.println("File not found");
                    e.printStackTrace();
                } catch (IOException e) {
                    System.out.println("Error accessing file");
                    e.printStackTrace();
                } catch (Exception e) {
                    System.out.println("Some error occured");
                    e.printStackTrace();
                }

            }
        }
        else if (resultCode == Activity.RESULT_OK && data!=null)
        {
            has_pickup_gps="1";

            pickup_gps_latitude = data.getStringExtra("latitude");
            pickup_gps_longitude = data.getStringExtra("longitude");
            latitude.setVisibility(View.VISIBLE);
            longitude.setVisibility(View.VISIBLE);
            latitude.setText(pickup_gps_latitude);
            longitude.setText(pickup_gps_longitude);
        }
    }



    public class uploadcloudinary extends AsyncTask<Void,Void,Void> {

        @Override
        protected Void doInBackground(Void... params) {
            Map config = new HashMap();
            config.put("cloud_name", "feedingindiaapp");
            config.put("api_key", "721272957494713");
            config.put("api_secret", "4Mr4HHRpMZ0aKABIuNIsDI5AZvw");
            Cloudinary cloudinary = new Cloudinary(config);
            /*
            Intent i=new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(Uri.fromFile(pic), "image/jpeg");
            startActivity(i);
            */
            try {
                cloudinary.uploader().upload(pic.getAbsolutePath(), ObjectUtils.asMap("public_id",finalurl));
                String[] htmlPicTag = cloudinary.url().imageTag(finalurl + ".jpg").split("'");

                foodPicUrl = htmlPicTag[1];
//                foodPicUrl = "hey.jpg";
//                System.out.println("Returned data is: " + htmlPicTag);
                System.out.println("Image url is " + foodPicUrl);

            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    };


    private class Bgtask extends AsyncTask<String[], Void, String[]> {

        @Override
        protected void onPreExecute() {

            progressDialog = new ProgressDialog(AddFood.this);
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            progressDialog.setTitle("Submitting your donation");
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setMessage("Please wait...");
            progressDialog.show();


        }

        @Override
        protected String[] doInBackground(String[]... passing) {


            String result[] =new String[1];
            String fetch_url = "https://feedingindiaapp.000webhostapp.com/adddata/addfood.php";
            try {


                URL url = new URL(fetch_url);
                HttpURLConnection http = (HttpURLConnection) url.openConnection();
                http.setRequestMethod("POST");
                http.setDoInput(true);
                http.setDoOutput(true);
                OutputStream os = http.getOutputStream();
                BufferedWriter bf = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                String[] details = passing[0];
                // getting String

                System.out.println("final url is 1: " + foodPicUrl);
                System.out.println("final url is 2: " + details[1]);

                String postdata = URLEncoder.encode("donor_id", "UTF-8") + "=" + URLEncoder.encode(details[0], "UTF-8") + "&" +
                        URLEncoder.encode("password_hash", "UTF-8") + "=" + URLEncoder.encode(password_hash, "UTF-8")+ "&" +
                        URLEncoder.encode("pickup_photo_url", "UTF-8") + "=" + URLEncoder.encode(foodPicUrl, "UTF-8") + "&" +
                        URLEncoder.encode("pickup_city", "UTF-8") + "=" + URLEncoder.encode(details[2], "UTF-8")+ "&" +
                        URLEncoder.encode("pickup_area", "UTF-8") + "=" + URLEncoder.encode(details[3], "UTF-8")+ "&" +
                        URLEncoder.encode("pickup_street", "UTF-8") + "=" + URLEncoder.encode(details[4], "UTF-8")+ "&" +
                        URLEncoder.encode("pickup_house_no", "UTF-8") + "=" + URLEncoder.encode(details[5], "UTF-8")+ "&" +
                        URLEncoder.encode("has_pickup_gps", "UTF-8") + "=" + URLEncoder.encode(details[6], "UTF-8")+ "&" +
                        URLEncoder.encode("pickup_gps_latitude", "UTF-8") + "=" + URLEncoder.encode(details[7], "UTF-8") + "&" +
                        URLEncoder.encode("pickup_gps_longitude", "UTF-8") + "=" + URLEncoder.encode(details[8], "UTF-8") + "&" +
                        URLEncoder.encode("is_veg", "UTF-8") + "=" + URLEncoder.encode(details[9], "UTF-8") + "&" +
                        URLEncoder.encode("is_perishable", "UTF-8") + "=" + URLEncoder.encode(details[10], "UTF-8") + "&" +
                        URLEncoder.encode("other_details", "UTF-8") + "=" + URLEncoder.encode(details[11], "UTF-8") ;


                bf.write(postdata);
                bf.flush();
                bf.close();
                os.close();

                InputStream is = http.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is, "iso-8859-1"));
                String line = "";

                while ((line = br.readLine()) != null) {
                    result[0] += line;

                }

                br.close();
                is.close();
                http.disconnect();
                return result;


            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }


            return null;
        }


        @Override
        protected void onPostExecute(String[] result) {

            progressDialog.dismiss();
            try {
                String jsonstring = result[0].substring(4,result[0].length());
                jsonObject = new JSONObject(jsonstring);
                jsonArray = jsonObject.getJSONArray("server_response");
                int count=0;
                String message;

                while(count<jsonArray.length())
                {
                    JSONObject jo = jsonArray.getJSONObject(count);
                    message = jo.getString("message");

                    if(message.equals("Some Server Error...Try again"))
                        Toast.makeText(AddFood.this, message, Toast.LENGTH_SHORT).show();

                    else if(message.equals("Food listed for donation...Thank you")) {
                        Toast.makeText(AddFood.this, "Your donation has been successfully posted. Thank You!!", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                    else{
                        Toast.makeText(AddFood.this, "It seems you have tried using wrong credentials. Please log in again!!", Toast.LENGTH_LONG).show();
                    }
                    count++;
                }



            } catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(AddFood.this, "Internal Error", Toast.LENGTH_SHORT).show();
            }

        }
    }


}



