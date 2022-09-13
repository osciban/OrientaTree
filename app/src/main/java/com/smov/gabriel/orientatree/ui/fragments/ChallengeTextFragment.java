package com.smov.gabriel.orientatree.ui.fragments;

import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.text.Normalizer;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.UUID;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.smov.gabriel.orientatree.R;
import com.smov.gabriel.orientatree.model.BeaconReached;
import com.smov.gabriel.orientatree.model.BeaconReachedLOD;
import com.smov.gabriel.orientatree.ui.ChallengeActivity;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ChallengeTextFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ChallengeTextFragment extends Fragment {

    private ChallengeActivity ca;

    private String right_answer;
    private String given_answer;
    private boolean givenAnswerIsRight;

    private BeaconReachedLOD beaconReached;

    private TextInputLayout challengeAnswer_textInputLayout;
    private Button challengeText_button;
    private CircularProgressIndicator challengeText_progressIndicator;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public ChallengeTextFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ChallengeTextFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ChallengeTextFragment newInstance(String param1, String param2) {
        ChallengeTextFragment fragment = new ChallengeTextFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_challenge_text, container, false);

        ca = (ChallengeActivity) getActivity();

        // get the right answer to the question
        right_answer = ca.beacon.getWritten_right_answer();

        // binding the view elements

        challengeAnswer_textInputLayout = view.findViewById(R.id.challengeAnswer_textInputLayout);
        challengeText_button = view.findViewById(R.id.challengeText_button);
        challengeText_progressIndicator = view.findViewById(R.id.challengeText_progressIndicator);




        String url = "http://192.168.137.1:8890/sparql?default-graph-uri=&query=SELECT+DISTINCT+%3FuserAnswer+WHERE%7B%0D%0A+%3Fbeacon%0D%0A++rdf%3AID+%22" + ca.beacon.getBeacon_id() + "%22.%0D%0A%3Fperson%0D%0A+ot%3AuserName+%22" + ca.userID + "%22.%0D%0A%3FpersonaAnswer%0D%0A+ot%3AtoThe+%3Fbeacon%3B%0D%0A+ot%3Aof+%3Fperson%3B%0D%0A+ot%3AanswerResource+%3FuserAnswer.%0D%0A%7D%0D%0A&format=json";
        RequestQueue queue = Volley.newRequestQueue(getContext());
        System.out.println("URL ChallengeQuizFragment2:" + url);
        beaconReached = new BeaconReachedLOD();
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray result = response.getJSONObject("results").getJSONArray("bindings");
                            if (result.length() > 0) {
                                for (int i = 0; i < result.length(); i++) {
                                    JSONObject aux = result.getJSONObject(i);
                                    String userAnswer = aux.getJSONObject("userAnswer").getString("value");
                                    beaconReached.setWritten_answer(userAnswer);

                                }
                                challengeAnswer_textInputLayout.getEditText()
                                        .setText(beaconReached.getWritten_answer());
                                if (beaconReached.getWritten_answer() != null) {
                                    if (beaconReached.getWritten_answer().equals(ca.beacon.getWritten_right_answer())) {
                                        beaconReached.setAnswer_right(true);
                                        displayPositiveFeedBack();
                                    } else {
                                        beaconReached.setAnswer_right(false);
                                        displayNegativeFeedBack();
                                    }

                                }
                            } else {
                                if (!ca.organizer) {
                                    Date current_time = new Date(System.currentTimeMillis());
                                    if (current_time.before(ca.activity.getFinishTime())) {
                                        // if not yet answered and we are not the planner,
                                        // and the activity didn't finish yet then enable actions and continue
                                        challengeAnswer_textInputLayout.setEnabled(true);
                                        challengeText_button.setEnabled(true);
                                    }
                                }
                            }

                        } catch (JSONException e) {
                            System.out.println(("noresponse"));
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO: Handle error

                    }
                });
        queue.add(jsonObjectRequest);

        // get the reach to check if already answered
        /*ca.db.collection("activities").document(ca.activityID)
                .collection("participations").document(ca.userID)
                .collection("beaconReaches").document(ca.beacon.getBeacon_id())
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        beaconReached = documentSnapshot.toObject(BeaconReached.class);
                        if (beaconReached.isAnswered()) {
                            // if already answered, don't enable actions and so the answer given instead
                            challengeAnswer_textInputLayout.getEditText()
                                    .setText(beaconReached.getWritten_answer());
                            if (beaconReached.isAnswer_right()) {
                                displayPositiveFeedBack();
                            } else {
                                displayNegativeFeedBack();
                            }
                        } else {
                            if (!ca.organizer) {
                                Date current_time = new Date(System.currentTimeMillis());
                                if (current_time.before(ca.activity.getFinishTime())) {
                                    // if not yet answered and we are not the planner,
                                    // and the activity didn't finish yet then enable actions and continue
                                    challengeAnswer_textInputLayout.setEnabled(true);
                                    challengeText_button.setEnabled(true);
                                }
                            }
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull @NotNull Exception e) {
                        Toast.makeText(ca, "Algo salió mal, vuelve a intentarlo", Toast.LENGTH_SHORT).show();
                    }
                });
*/
        // button listener
        challengeText_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new MaterialAlertDialogBuilder(getContext())
                        .setTitle("Envío de respuesta")
                        .setMessage("¿Desea enviar su respuesta?")
                        .setNegativeButton("Cancelar", null)
                        .setPositiveButton("Enviar", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                given_answer = challengeAnswer_textInputLayout.getEditText().getText().toString().trim();
                                if (given_answer.length() == 0) {
                                    challengeAnswer_textInputLayout.setError("No se puede dejar este campo vacío");
                                    challengeAnswer_textInputLayout.setErrorEnabled(true);
                                } else {
                                    challengeAnswer_textInputLayout.setErrorEnabled(false);
                                    // check if the given answer is numeric
                                    boolean numericAnswer = isNumeric(given_answer);
                                    if (numericAnswer) {
                                        // if numeric, correction with numeric criteria
                                        if (given_answer.equals(right_answer)) {
                                            givenAnswerIsRight = true;
                                            updateBeaconReach();
                                        } else {
                                            givenAnswerIsRight = false;
                                            updateBeaconReach();
                                        }
                                    } else {
                                        // if given answer is not numeric, correction with textual criteria
                                        // the smaller ratio is, the more similar is its length
                                        // if ratio is too big, we consider it wrong even if one contains the other
                                        double ratio = Math.abs(1 - (given_answer.length() / (double) right_answer.length()));
                                        // convert the given and right answers to uppercase
                                        String temp_given_answer = stripAccents(given_answer.toUpperCase());
                                        String temp_right_answer = stripAccents(right_answer.toUpperCase());
                                        if ((temp_given_answer.contains(temp_right_answer) ||
                                                temp_right_answer.contains(temp_given_answer)) && ratio <= 0.25) {
                                            // if one contains the other and they both have similar length, we consider them right
                                            givenAnswerIsRight = true;
                                            updateBeaconReach();
                                            //Toast.makeText(ca, "Correcto: " + temp_given_answer + " y " + temp_right_answer, Toast.LENGTH_SHORT).show();
                                        } else {
                                            givenAnswerIsRight = false;
                                            updateBeaconReach();
                                            //Toast.makeText(ca, "INcorrecto: " + temp_given_answer + " y " + temp_right_answer, Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }

                            }
                        })
                        .show();
            }
        });

        return view;
    }

    // function to check if the answer is numeric or not
    public static boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            double d = Double.parseDouble(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    // remove accents from a String
    public static String stripAccents(String s) {
        s = Normalizer.normalize(s, Normalizer.Form.NFD);
        s = s.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        return s;
    }

    // write in Firestore the answer given by the user, give some feedback and allow to continue
    public void updateBeaconReach() {
        challengeText_progressIndicator.setVisibility(View.VISIBLE);

        String url = "http://192.168.137.1:8890/sparql?default-graph-uri=&query=SELECT+DISTINCT+%3FpersonAnswer+WHERE%7B%0D%0A%3FpersonAnswer%0D%0Aot%3Aof+%3Fpersona%3B%0D%0Aot%3AtoThe+%3Fbeacon.%0D%0A%3Fpersona%0D%0Aot%3AuserName+%22" + ca.userID + "%22.%0D%0A%3Fbeacon%0D%0Ardf%3AID+%22" + ca.beacon.getBeacon_id() + "%22.%0D%0A%7D&format=json";

        System.out.println("obtenerIRISChallengeText:" + url);
        RequestQueue queue = Volley.newRequestQueue(getContext());

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray result = response.getJSONObject("results").getJSONArray("bindings");
                            String personAnswerIRI = "";
                            for (int i = 0; i < result.length(); i++) {
                                JSONObject aux = result.getJSONObject(i);
                                personAnswerIRI = aux.getJSONObject("personAnswer").getString("value").split("#")[1];
                            }
                            String url = "http://192.168.137.1:8890/sparql?default-graph-uri=&query=INSERT+DATA%7B%0D%0A++GRAPH+%3Chttp%3A%2F%2Flocalhost%3A8890%2FDAV%3E+%7B%0D%0A+++ot%3A" + personAnswerIRI + "+ot%3AanswerResource+\"" + given_answer + "\".%0D%0A+++%7D%0D%0A%7D%0D%0A&format=json";

                            System.out.println("challengeTextUpload:" + url);
                            RequestQueue queue = Volley.newRequestQueue(getContext());

                            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                                    (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                                        @Override
                                        public void onResponse(JSONObject response) {
                                            try {
                                                String result = response.getJSONObject("results").getJSONArray("bindings").getJSONObject(0).getJSONObject("callret-0").getString("value");
                                                // not uploading any more
                                                if (result.equals("Insert into <http://localhost:8890/DAV>, 1 (or less) triples -- done")) {
                                                    System.out.println("AQUI LLEGO  CHALLENGE");
                                                    challengeText_progressIndicator.setVisibility(View.GONE);
                                                    challengeText_button.setEnabled(false);
                                                    challengeAnswer_textInputLayout.setEnabled(false);
                                                    // give some feedback
                                                    if (givenAnswerIsRight) {
                                                        displayPositiveFeedBack();
                                                    } else {
                                                        displayNegativeFeedBack();
                                                    }
                                                } else {
                                                    challengeText_progressIndicator.setVisibility(View.GONE);
                                                    Toast.makeText(ca, "Algo salió mal, vuelva a intentarlo.", Toast.LENGTH_SHORT).show();
                                                }


                                            } catch (JSONException e) {
                                                System.out.println(("noresponse"));
                                                e.printStackTrace();
                                            }

                                        }
                                    }, new Response.ErrorListener() {

                                        @Override
                                        public void onErrorResponse(VolleyError error) {
                                            // TODO: Handle error

                                        }
                                    });
                            queue.add(jsonObjectRequest);


                        } catch (JSONException e) {
                            System.out.println(("noresponse"));
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO: Handle error

                    }
                });
        queue.add(jsonObjectRequest);


        /*ca.db.collection("activities").document(ca.activityID)
                .collection("participations").document(ca.userID)
                .collection("beaconReaches").document(ca.beacon.getBeacon_id())
                .update("answer_right", givenAnswerIsRight,
                        "written_answer", given_answer,
                        "answered", true)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        challengeText_progressIndicator.setVisibility(View.GONE);
                        challengeText_button.setEnabled(false);
                        challengeAnswer_textInputLayout.setEnabled(false);
                        // give some feedback
                        if (givenAnswerIsRight) {
                            displayPositiveFeedBack();
                        } else {
                            displayNegativeFeedBack();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull @NotNull Exception e) {
                        challengeText_progressIndicator.setVisibility(View.GONE);
                        Toast.makeText(ca, "Algo salió mal, vuelva a intentarlo.", Toast.LENGTH_SHORT).show();
                    }
                });*/
    }

    private void displayNegativeFeedBack() {
        challengeAnswer_textInputLayout.setErrorIconDrawable(R.drawable.ic_close);
        challengeAnswer_textInputLayout.setError("La respuesta correcta es: " + right_answer);
        challengeAnswer_textInputLayout.setErrorEnabled(true);
    }

    private void displayPositiveFeedBack() {
        challengeAnswer_textInputLayout.setEndIconActivated(true);
        challengeAnswer_textInputLayout.setHelperText("Respuesta correcta");
        challengeAnswer_textInputLayout.setHelperTextEnabled(true);
        challengeAnswer_textInputLayout.setEndIconDrawable(R.drawable.ic_check);
    }

}