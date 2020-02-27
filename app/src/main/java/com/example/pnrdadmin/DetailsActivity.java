package com.example.pnrdadmin;

        import androidx.annotation.NonNull;
        import androidx.appcompat.app.AppCompatActivity;

        import android.content.Intent;
        import android.os.Bundle;
        import android.view.View;
        import android.widget.ArrayAdapter;
        import android.widget.Button;
        import android.widget.Spinner;
        import android.widget.Toast;

        import com.google.android.gms.tasks.OnFailureListener;
        import com.google.android.gms.tasks.OnSuccessListener;
        import com.google.firebase.firestore.DocumentReference;
        import com.google.firebase.firestore.FirebaseFirestore;

        import java.util.HashMap;
        import java.util.Map;

public class DetailsActivity extends AppCompatActivity {

    private Spinner spinner;
    private Button submitButton;
    private String serviceProvider;
    private FirebaseFirestore firebaseFirestore;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
        //serviceProvider Spinner List

        firebaseFirestore = FirebaseFirestore.getInstance();
        spinner = findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,R.array.service_providers,R.layout.support_simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        // serviceProvider Spinner List ends Here

        submitButton = findViewById(R.id.submitButton);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                serviceProvider = spinner.getSelectedItem().toString();
                if(serviceProvider.equals("None")){
                    //if no service provider is selected display error message
                    Toast.makeText(getApplicationContext(),"Please choose Service Provider",Toast.LENGTH_SHORT).show();


                }
                else
                {
                    //provide the user with the heatmap of the selected network
                    if(spinner.getSelectedItem().toString().equals("Airtel"))
                    {
                        Map<String,String> providersMap =  new HashMap<>();
                        providersMap.put("provider_name",spinner.getSelectedItem().toString());
                       DocumentReference documentReference =  firebaseFirestore.collection("airtel")
                            .document("122");

                                documentReference.set(providersMap)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Toast.makeText(getApplicationContext(),"Success",Toast.LENGTH_SHORT).show();

                                        Intent intent = new Intent(DetailsActivity.this,HeatmapsDemoActivity.class);
                                        startActivity(intent);

                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(getApplicationContext(),"Failurer",Toast.LENGTH_SHORT).show();

                            }
                        });
                    }
                    else if(spinner.getSelectedItem().toString().equals("BSNL"))
                    {
                        Map<String,String> providersMap =  new HashMap<>();
                        providersMap.put("provider_name",spinner.getSelectedItem().toString());
                        DocumentReference documentReference =  firebaseFirestore.collection("bsnl")
                                .document("122");

                        documentReference.set(providersMap)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Toast.makeText(getApplicationContext(),"Data updated Successfully",Toast.LENGTH_SHORT).show();

                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(getApplicationContext(),"Failed to update data",Toast.LENGTH_SHORT).show();

                            }
                        });
                    }


                }

            }
        });


    }
}
