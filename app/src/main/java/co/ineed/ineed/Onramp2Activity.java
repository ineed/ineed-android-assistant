package co.ineed.ineed;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

/**
 * Created by John on 11/07/2015.
 */
public class Onramp2Activity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.onramp2_activity);

        Button btn = (Button) findViewById(R.id.buttonNext);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Onramp2Activity.this, CreateAccountActivity.class);
                startActivity(intent);
            }
        });
    }
}
