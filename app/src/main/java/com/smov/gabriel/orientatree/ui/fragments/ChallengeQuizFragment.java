package com.smov.gabriel.orientatree.ui.fragments;

import android.content.DialogInterface;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.smov.gabriel.orientatree.R;
import com.smov.gabriel.orientatree.model.BeaconReachedLOD;
import com.smov.gabriel.orientatree.ui.ChallengeActivity;
import com.smov.gabriel.orientatree.utils.MySingleton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ChallengeQuizFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ChallengeQuizFragment extends Fragment {

    private ChallengeActivity ca;

    private Button challengeQuiz_button;
    private CircularProgressIndicator challengeQuiz_progressIndicator;
    private RadioButton quiz_radioButton_0, quiz_radioButton_1, quiz_radioButton_2, quiz_radioButton_3;
    private RadioGroup quiz_radioGroup;

    private BeaconReachedLOD beaconReached;

    private int radioButton_selected = 0;
    private boolean givenAnswerIsRight = false;

    // here we store the possible_answers
    private ArrayList<String> possible_answers;

    // flag to know if we successfully got the possible_answers or not
    private boolean possible_answers_set = false;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public ChallengeQuizFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ChallengeQuizFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ChallengeQuizFragment newInstance(String param1, String param2) {
        ChallengeQuizFragment fragment = new ChallengeQuizFragment();
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
        View view = inflater.inflate(R.layout.fragment_challenge_quiz, container, false);

        ca = (ChallengeActivity) getActivity();

        // bind interface elements
        challengeQuiz_button = view.findViewById(R.id.challengeQuiz_button);
        challengeQuiz_progressIndicator = view.findViewById(R.id.challengeQuiz_progressIndicator);
        quiz_radioButton_0 = view.findViewById(R.id.quiz_radio_button_0);
        quiz_radioButton_1 = view.findViewById(R.id.quiz_radio_button_1);
        quiz_radioButton_2 = view.findViewById(R.id.quiz_radio_button_2);
        quiz_radioButton_3 = view.findViewById(R.id.quiz_radio_button_3);
        quiz_radioGroup = view.findViewById(R.id.quiz_radioGroup);

        // set the text for the different options

        if (ca.beacon != null) {

            possible_answers = ca.beacon.getPossible_answers();
            if (possible_answers != null) {

                if (possible_answers.size() == 4) {

                    quiz_radioButton_0.setText(possible_answers.get(0));
                    quiz_radioButton_1.setText(possible_answers.get(1));
                    quiz_radioButton_2.setText(possible_answers.get(2));
                    quiz_radioButton_3.setText(possible_answers.get(3));
                    possible_answers_set = true;
                }
            }
        }

        // notify the user in case that the different options couldn't be set
        if (!possible_answers_set) {
            Toast.makeText(ca, "Algo salió mal al cargar las posibles respuestas", Toast.LENGTH_SHORT).show();
        }

        beaconReachedOperations();

        // get the reach to check if already answered

        // radio group listener
        quiz_radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // when we check a radio button, the answer button is enabled
                challengeQuiz_button.setEnabled(true);
                switch (checkedId) {
                    case R.id.quiz_radio_button_0:
                        radioButton_selected = 0;
                        break;
                    case R.id.quiz_radio_button_1:
                        radioButton_selected = 1;
                        break;
                    case R.id.quiz_radio_button_2:
                        radioButton_selected = 2;
                        break;
                    case R.id.quiz_radio_button_3:
                        radioButton_selected = 3;
                        break;
                    default:
                        break;
                }
            }
        });

        challengeQuiz_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new MaterialAlertDialogBuilder(getContext())
                        .setTitle("Envío de respuesta")
                        .setMessage("¿Desea enviar su respuesta?")
                        .setNegativeButton("Cancelar", null)
                        .setPositiveButton("Enviar", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                challengeQuiz_progressIndicator.setVisibility(View.VISIBLE);
                                // check if the given answer is right
                                if (radioButton_selected == ca.beacon.getQuiz_right_answer()) {
                                    givenAnswerIsRight = true;
                                } else {
                                    givenAnswerIsRight = false;
                                }
                                updateBeaconReach();
                            }
                        })
                        .show();
            }
        });

        return view;
    }

    private void beaconReachedOperations() {

        /*
        * SELECT DISTINCT ?userAnswer WHERE {
        *   ?beacon
        *       rdf:ID ca.beacon.getBeacon_id().
        *   ?person
        *       ot:userName ca.userID.
        *   ?personaAnswer
        *       ot:toThe ?beacon;
        *       ot:of ?person;
        *       ot:answerResource ?userAnswer.
        *  }
        *
        * */

        String url = "http://192.168.137.1:8890/sparql?default-graph-uri=&query=SELECT+DISTINCT+%3FuserAnswer+WHERE%7B%0D%0A+%3Fbeacon%0D%0A++rdf%3AID+%22" + ca.beacon.getBeacon_id() + "%22.%0D%0A%3Fperson%0D%0A+ot%3AuserName+%22" + ca.userID + "%22.%0D%0A%3FpersonaAnswer%0D%0A+ot%3AtoThe+%3Fbeacon%3B%0D%0A+ot%3Aof+%3Fperson%3B%0D%0A+ot%3AanswerResource+%3FuserAnswer.%0D%0A%7D%0D%0A&format=json";
        beaconReached=new BeaconReachedLOD();
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray result = response.getJSONObject("results").getJSONArray("bindings");
                            for (int i = 0; i < result.length(); i++) {
                                JSONObject aux = result.getJSONObject(i);

                                String userAnswer=aux.getJSONObject("userAnswer").getString("value");
                                beaconReached.setWritten_answer(userAnswer);

                            }
                            if (beaconReached.getWritten_answer()!=null) {
                                if(beaconReached.getWritten_answer().equals(ca.beacon.getWritten_right_answer())){
                                    beaconReached.setAnswer_right(true);
                                }else{
                                    beaconReached.setAnswer_right(false);
                                }
                                for(int i=0; i<possible_answers.size();i++){
                                    if (possible_answers.get(i).equals(beaconReached.getWritten_answer())){
                                        beaconReached.setQuiz_answer(i);
                                        break;
                                    }
                                }
                                // if the reach has already been answered, get what the user answered
                                // and show some feedback, but without enabling any actions
                                radioButton_selected = beaconReached.getQuiz_answer();
                                if (beaconReached.isAnswer_right()) {
                                    showPositiveFeedBack();
                                } else {
                                    showNegativeFeedBack();
                                }
                            } else {
                                if (!ca.organizer) {
                                    Date current_time = new Date(System.currentTimeMillis());
                                    if (current_time.before(ca.activity.getFinishTime())) {
                                        // if the reach has not been answered yet, and we are not the organizer
                                        // and the activity didn't finish yet then enable the radio buttons
                                        quiz_radioButton_0.setClickable(true);
                                        quiz_radioButton_1.setClickable(true);
                                        quiz_radioButton_2.setClickable(true);
                                        quiz_radioButton_3.setClickable(true);
                                    }
                                }
                            }
                        } catch (JSONException e) {
                            Log.d("TAG","norespone");
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("TAG","norespone");

                    }
                });
        MySingleton.getInstance(getContext()).addToRequestQueue(jsonObjectRequest);



    }

    private void updateBeaconReach() {

        /*
        *
        * SELECT DISTINCT ?personAnswer WHERE{
        *   ?personAnswer
        *       ot:of ?persona;
        *       ot:toThe ?beacon.
        *   ?persona
        *       ot:userName ca.userID.
        *   ?beacon
        *       rdf:ID  ca.beacon.getBeacon_id().
        * }
        *
        */
        String url = "http://192.168.137.1:8890/sparql?default-graph-uri=&query=SELECT+DISTINCT+%3FpersonAnswer+WHERE%7B%0D%0A%3FpersonAnswer%0D%0Aot%3Aof+%3Fpersona%3B%0D%0Aot%3AtoThe+%3Fbeacon.%0D%0A%3Fpersona%0D%0Aot%3AuserName+%22" + ca.userID + "%22.%0D%0A%3Fbeacon%0D%0Ardf%3AID+%22" + ca.beacon.getBeacon_id() + "%22.%0D%0A%7D&format=json";

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

                            /*
                            *
                            * INSERT DATA {
                            *   GRAPH <http://localhost/DAV> {
                            *       ot:personAnwerIRI ot:answerResource possible_answers.get(beaconReached.getQuiz_answer()).
                            *   }
                            * }
                            *
                            */

                            String url = "http://192.168.137.1:8890/sparql?default-graph-uri=&query=INSERT+DATA%7B%0D%0A+GRAPH+%3Chttp%3A%2F%2Flocalhost%3A8890%2FDAV%3E+%7B%0D%0A+ot%3A" + personAnswerIRI + "+ot%3AanswerResource+\"" + possible_answers.get(beaconReached.getQuiz_answer()) + "\".%0D%0A+++%7D%0D%0A%7D%0D%0A&format=json";

                            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                                    (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                                        @Override
                                        public void onResponse(JSONObject response) {
                                            try {
                                                String result = response.getJSONObject("results").getJSONArray("bindings").getJSONObject(0).getJSONObject("callret-0").getString("value");
                                                // not uploading any more
                                                if (result.equals("Insert into <http://localhost:8890/DAV>, 1 (or less) triples -- done")) {
                                                    challengeQuiz_progressIndicator.setVisibility(View.GONE);
                                                    challengeQuiz_button.setEnabled(false);
                                                    quiz_radioGroup.setEnabled(false);
                                                    quiz_radioButton_0.setClickable(false);
                                                    quiz_radioButton_1.setClickable(false);
                                                    quiz_radioButton_2.setClickable(false);
                                                    quiz_radioButton_3.setClickable(false);
                                                    if (givenAnswerIsRight) {
                                                        showPositiveFeedBack();
                                                    } else {
                                                        showNegativeFeedBack();
                                                    }
                                                } else {
                                                    challengeQuiz_progressIndicator.setVisibility(View.GONE);
                                                    Toast.makeText(ca, "Algo salió mal, vuelve a intentarlo", Toast.LENGTH_SHORT).show();
                                                }


                                            } catch (JSONException e) {
                                                challengeQuiz_progressIndicator.setVisibility(View.GONE);
                                                Toast.makeText(ca, "Algo salió mal, vuelve a intentarlo", Toast.LENGTH_SHORT).show();
                                            }

                                        }
                                    }, new Response.ErrorListener() {

                                        @Override
                                        public void onErrorResponse(VolleyError error) {
                                            challengeQuiz_progressIndicator.setVisibility(View.GONE);
                                            Toast.makeText(ca, "Algo salió mal, vuelve a intentarlo", Toast.LENGTH_SHORT).show();

                                        }
                                    });
                            MySingleton.getInstance(getContext()).addToRequestQueue(jsonObjectRequest);


                        } catch (JSONException e) {
                            Log.d("TAG","norespone");
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("TAG","norespone");

                    }
                });
        MySingleton.getInstance(getContext()).addToRequestQueue(jsonObjectRequest);

    }

    private void showNegativeFeedBack() {
        switch (radioButton_selected) {
            case 0:
                quiz_radioButton_0.setTextColor(getResources().getColor(R.color.error_red));
                quiz_radioButton_0.append("\n INCORRECTO");
                break;
            case 1:
                quiz_radioButton_1.setTextColor(getResources().getColor(R.color.error_red));
                quiz_radioButton_1.append("\n INCORRECTO");
                break;
            case 2:
                quiz_radioButton_2.setTextColor(getResources().getColor(R.color.error_red));
                quiz_radioButton_2.append("\n INCORRECTO");
                break;
            case 3:
                quiz_radioButton_3.setTextColor(getResources().getColor(R.color.error_red));
                quiz_radioButton_3.append("\n INCORRECTO");
                break;
        }
        switch (ca.beacon.getQuiz_right_answer()) {
            case 0:
                quiz_radioButton_0.setTextColor(getResources().getColor(R.color.secondary_color_variant));
                quiz_radioButton_0.append("\n La respuesta correcta era esta");
                break;
            case 1:
                quiz_radioButton_1.setTextColor(getResources().getColor(R.color.secondary_color_variant));
                quiz_radioButton_1.append("\n La respuesta correcta era esta");
                break;
            case 2:
                quiz_radioButton_2.setTextColor(getResources().getColor(R.color.secondary_color_variant));
                quiz_radioButton_2.append("\n La respuesta correcta era esta");
                break;
            case 3:
                quiz_radioButton_3.setTextColor(getResources().getColor(R.color.secondary_color_variant));
                quiz_radioButton_3.append("\n La respuesta correcta era esta");
                break;
            default:
                break;
        }
    }

    private void showPositiveFeedBack() {
        switch (radioButton_selected) {
            case 0:
                quiz_radioButton_0.setTextColor(getResources().getColor(R.color.primary_color));
                quiz_radioButton_0.append("\n  CORRECTO");
                break;
            case 1:
                quiz_radioButton_1.setTextColor(getResources().getColor(R.color.primary_color));
                quiz_radioButton_1.append("\n  CORRECTO");
                break;
            case 2:
                quiz_radioButton_2.setTextColor(getResources().getColor(R.color.primary_color));
                quiz_radioButton_2.append("\n  CORRECTO");
                break;
            case 3:
                quiz_radioButton_3.setTextColor(getResources().getColor(R.color.primary_color));
                quiz_radioButton_3.append("\n  CORRECTO");
                break;
            default:
                break;
        }
    }


}