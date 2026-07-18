package it.dirigenti.quiz;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {
    private static final String ALL_AREAS = "Tutte le aree";
    private static final int BLUE = Color.rgb(21, 101, 192);
    private static final int DARK = Color.rgb(33, 33, 33);
    private static final int MUTED = Color.rgb(97, 97, 97);
    private static final int BG = Color.rgb(245, 247, 251);
    private static final int CARD = Color.WHITE;
    private static final int GOOD = Color.rgb(46, 125, 50);
    private static final int BAD = Color.rgb(198, 40, 40);

    static class Question {
        String id;
        String rif;
        String area;
        String areaName;
        String question;
        String explanation;
        String[] answers;
        int correct;
    }

    private final ArrayList<Question> all = new ArrayList<>();
    private final ArrayList<Question> deck = new ArrayList<>();
    private final Map<String, String> areaNames = new LinkedHashMap<>();
    private final ArrayList<Button> answerButtons = new ArrayList<>();

    private LinearLayout root;
    private Spinner areaSpinner;
    private TextView subtitle;
    private TextView scoreView;
    private TextView progressView;
    private TextView questionView;
    private TextView feedbackView;
    private TextView areaBadge;
    private ProgressBar progressBar;
    private LinearLayout answersBox;
    private Button primaryButton;
    private Button explanationButton;
    private Button resetButton;

    private int index = 0;
    private int correctCount = 0;
    private int wrongCount = 0;
    private boolean quizStarted = false;
    private boolean answered = false;
    private String selectedArea = ALL_AREAS;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        loadQuestions();
        buildLayout();
        showHome();
    }

    private void buildLayout() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(BG);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(22), dp(20), dp(22));
        scrollView.addView(root);
        setContentView(scrollView);
    }

    private void showHome() {
        quizStarted = false;
        answered = false;
        root.removeAllViews();

        TextView title = text("Dirigenti Quiz", 30, true, BLUE);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title);

        subtitle = text("Allenamento mirato sulla banca dati ufficiale, con risposte mischiate e punteggio immediato.", 16, false, MUTED);
        subtitle.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(subtitle);

        LinearLayout card = card();
        card.addView(text("Scegli l'area tematica", 18, true, DARK));
        card.addView(text(all.size() + " quesiti disponibili • funzionamento offline", 14, false, MUTED));

        areaSpinner = new Spinner(this);
        ArrayList<String> labels = areaLabels();
        areaSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, labels));
        areaSpinner.setPadding(0, dp(8), 0, dp(8));
        areaSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedArea = labels.get(position);
                updateHomeSummary(card);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedArea = ALL_AREAS;
            }
        });
        card.addView(areaSpinner);

        TextView tip = text("Consiglio: parti da un'area singola, poi passa a tutte le aree per simulare una prova mista.", 14, false, MUTED);
        tip.setPadding(0, dp(10), 0, dp(4));
        card.addView(tip);
        root.addView(card);

        primaryButton = mainButton("Inizia allenamento");
        primaryButton.setOnClickListener(view -> startQuiz());
        root.addView(primaryButton);
    }

    private void updateHomeSummary(LinearLayout card) {
        if (card.getChildCount() < 2) {
            return;
        }
        int count = countForSelectedArea();
        ((TextView) card.getChildAt(1)).setText(count + " quesiti disponibili • funzionamento offline");
    }

    private void startQuiz() {
        deck.clear();
        String selectedCode = areaCodeFromLabel(selectedArea);
        for (Question question : all) {
            if (selectedCode == null || question.area.equals(selectedCode)) {
                deck.add(question);
            }
        }
        Collections.shuffle(deck);
        index = 0;
        correctCount = 0;
        wrongCount = 0;
        quizStarted = true;
        answered = false;
        buildQuizScreen();
        showQuestion();
    }

    private void buildQuizScreen() {
        root.removeAllViews();

        LinearLayout header = card();
        TextView title = text("Allenamento", 24, true, BLUE);
        header.addView(title);
        areaBadge = text(selectedArea, 14, true, MUTED);
        header.addView(areaBadge);
        scoreView = text("", 16, true, DARK);
        header.addView(scoreView);
        progressView = text("", 14, false, MUTED);
        header.addView(progressView);
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        header.addView(progressBar);
        root.addView(header);

        LinearLayout questionCard = card();
        questionView = text("", 19, false, DARK);
        questionView.setLineSpacing(0, 1.12f);
        questionCard.addView(questionView);
        answersBox = new LinearLayout(this);
        answersBox.setOrientation(LinearLayout.VERTICAL);
        questionCard.addView(answersBox);
        feedbackView = text("", 15, true, MUTED);
        questionCard.addView(feedbackView);
        explanationButton = secondaryButton("Spiegazione");
        explanationButton.setVisibility(View.GONE);
        questionCard.addView(explanationButton);
        root.addView(questionCard);

        primaryButton = mainButton("Prossima domanda");
        primaryButton.setVisibility(View.GONE);
        primaryButton.setOnClickListener(view -> nextQuestion());
        root.addView(primaryButton);

        resetButton = secondaryButton("Termina e cambia area");
        resetButton.setOnClickListener(view -> showHome());
        root.addView(resetButton);
    }

    private void showQuestion() {
        answersBox.removeAllViews();
        answerButtons.clear();
        feedbackView.setText("");
        primaryButton.setVisibility(View.GONE);
        if (explanationButton != null) {
            explanationButton.setVisibility(View.GONE);
            explanationButton.setOnClickListener(null);
        }
        answered = false;

        if (deck.isEmpty()) {
            questionView.setText("Nessuna domanda disponibile per questa area.");
            return;
        }

        Question question = deck.get(index);
        scoreView.setText(scoreText());
        progressView.setText("Domanda " + (index + 1) + " di " + deck.size() + " • Rif. " + question.rif);
        progressBar.setProgress((int) (((index + 1) * 100f) / deck.size()));
        areaBadge.setText(question.areaName);
        questionView.setText(question.question);

        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < question.answers.length; i++) {
            order.add(i);
        }
        Collections.shuffle(order);
        for (int answerIndex : order) {
            Button button = secondaryButton(question.answers[answerIndex]);
            button.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
            button.setOnClickListener(view -> answer(question, answerIndex));
            answersBox.addView(button);
            answerButtons.add(button);
        }
    }

    private void answer(Question question, int answerIndex) {
        if (answered) {
            return;
        }
        answered = true;
        boolean isCorrect = answerIndex == question.correct;
        if (isCorrect) {
            correctCount++;
            feedbackView.setTextColor(GOOD);
            feedbackView.setText("Corretto. Continua così.");
        } else {
            wrongCount++;
            feedbackView.setTextColor(BAD);
            feedbackView.setText("Errato. Risposta corretta: " + question.answers[question.correct]);
        }
        scoreView.setText(scoreText());
        for (Button button : answerButtons) {
            button.setEnabled(false);
        }
        explanationButton.setOnClickListener(view -> showExplanation(question));
        explanationButton.setVisibility(View.VISIBLE);
        primaryButton.setText(index + 1 >= deck.size() ? "Vedi riepilogo" : "Prossima domanda");
        primaryButton.setVisibility(View.VISIBLE);
    }

    private void showExplanation(Question question) {
        new AlertDialog.Builder(this)
                .setTitle("Spiegazione")
                .setMessage(question.explanation)
                .setPositiveButton("Ho capito", null)
                .show();
    }

    private void nextQuestion() {
        index++;
        if (index >= deck.size()) {
            showSummary();
        } else {
            showQuestion();
        }
    }

    private void showSummary() {
        root.removeAllViews();
        LinearLayout card = card();
        card.addView(text("Riepilogo allenamento", 24, true, BLUE));
        card.addView(text(selectedArea, 15, true, MUTED));
        card.addView(text(scoreText(), 20, true, DARK));
        int total = correctCount + wrongCount;
        int percent = total == 0 ? 0 : (correctCount * 100 / total);
        String level = percent >= 85 ? "Preparazione molto buona" : percent >= 70 ? "Preparazione discreta" : percent >= 55 ? "Da consolidare" : "Serve ripasso mirato";
        card.addView(text(level, 18, true, percent >= 70 ? GOOD : BAD));
        root.addView(card);

        Button retry = mainButton("Ripeti stessa area");
        retry.setOnClickListener(view -> startQuiz());
        root.addView(retry);

        Button change = secondaryButton("Cambia area");
        change.setOnClickListener(view -> showHome());
        root.addView(change);
    }

    private String scoreText() {
        int total = correctCount + wrongCount;
        String percent = total == 0 ? "n/d" : (correctCount * 100 / total) + "%";
        return "Corrette " + correctCount + " • Errate " + wrongCount + " • Preparazione " + percent;
    }

    private ArrayList<String> areaLabels() {
        ArrayList<String> labels = new ArrayList<>();
        labels.add(ALL_AREAS);
        areaNames.clear();
        for (Question question : all) {
            areaNames.put(question.area, question.areaName);
        }
        for (Map.Entry<String, String> entry : areaNames.entrySet()) {
            labels.add("Area " + entry.getKey() + " — " + entry.getValue());
        }
        return labels;
    }

    private int countForSelectedArea() {
        String code = areaCodeFromLabel(selectedArea);
        if (code == null) {
            return all.size();
        }
        int count = 0;
        for (Question question : all) {
            if (question.area.equals(code)) {
                count++;
            }
        }
        return count;
    }

    private String areaCodeFromLabel(String label) {
        if (label == null || label.equals(ALL_AREAS)) {
            return null;
        }
        int start = label.indexOf(' ') + 1;
        int end = label.indexOf('—');
        if (start <= 0 || end <= start) {
            return null;
        }
        return label.substring(start, end).trim();
    }

    private LinearLayout card() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(18), dp(16), dp(18), dp(16));
        layout.setBackgroundColor(CARD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(14), 0, dp(14));
        layout.setLayoutParams(params);
        return layout;
    }

    private TextView text(String value, int size, boolean bold, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setPadding(0, dp(5), 0, dp(5));
        if (bold) {
            view.setTypeface(Typeface.DEFAULT_BOLD);
        }
        return view;
    }

    private Button mainButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextColor(Color.WHITE);
        button.setTextSize(16);
        button.setBackgroundColor(BLUE);
        button.setPadding(dp(8), dp(10), dp(8), dp(10));
        button.setLayoutParams(buttonParams());
        return button;
    }

    private Button secondaryButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(15);
        button.setTextColor(DARK);
        button.setPadding(dp(8), dp(8), dp(8), dp(8));
        button.setLayoutParams(buttonParams());
        return button;
    }

    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(8), 0, dp(4));
        return params;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void loadQuestions() {
        try {
            InputStream inputStream = getAssets().open("questions.json");
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) > 0) {
                output.write(buffer, 0, read);
            }
            JSONArray array = new JSONArray(output.toString(StandardCharsets.UTF_8.name()));
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                Question question = new Question();
                question.id = object.getString("id");
                question.rif = object.getString("rif");
                question.area = object.getString("area");
                question.areaName = object.getString("areaName");
                question.question = object.getString("question");
                question.explanation = object.optString("explanation", "La risposta corretta è: " + object.getJSONArray("answers").getString(0));
                JSONArray answers = object.getJSONArray("answers");
                question.answers = new String[]{answers.getString(0), answers.getString(1), answers.getString(2), answers.getString(3)};
                question.correct = object.getInt("correct");
                all.add(question);
            }
        } catch (Exception exception) {
            Toast.makeText(this, "Errore nel caricamento delle domande", Toast.LENGTH_LONG).show();
            throw new RuntimeException(exception);
        }
    }
}
