package com.example.android.stationfinder;

import android.support.test.espresso.action.ViewActions;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)

public class Main2ActivityTest {

    //by adding an activity test rule i am telling the runner to launch this activity before any tests and tear it down after they finish

    @Rule
    public ActivityTestRule<Main2Activity> main2ActivityActivityTestRule =
            new ActivityTestRule<>(Main2Activity.class);

    // @Rule
    // public IntentsTestRule<Main2Activity> main2ActivityIntentsTestRule =
    //      new IntentsTestRule<>(Main2Activity.class);


    @Test
    public void enterSingleRbl() {

        /*Intent resultData = new Intent();
        String rbl = "132";
        resultData.putExtra("rbl", rbl);
        Instrumentation.ActivityResult result = new Instrumentation.ActivityResult(Activity.RESULT_OK, resultData);

        intending(toPackage("com.example.android.stationfinder")).respondWith(result);

*/

        onView(withId(R.id.et_rbl)).perform(typeText("   ")).perform(ViewActions.closeSoftKeyboard());
        onView(withId(R.id.btn_rbl)).perform(click());


       /* intended(allOf(hasComponent(hasShortClassName(".StationActivity")),
                toPackage("com.example.android.stationfinder"),
                hasExtra("rbls", "123")));*/


    }


}
