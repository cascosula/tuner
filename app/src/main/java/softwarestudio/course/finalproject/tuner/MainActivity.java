package softwarestudio.course.finalproject.tuner;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import softwarestudio.course.finalproject.tuner.Tuner.FrequencyAnalysisFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new FrequencyAnalysisFragment())
                    .commit();
        }
    }
}
