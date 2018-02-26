package com.example.android.stationfinder;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.Button;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


/***
 *
 * Die Menu Activity ist der Einstiegspunkt für die App
 *
 * Hier findet man, (abgesehen von ein bisschen Product Branding ;-) ein Textfeld mit Anweisung und zwei Buttons
 *
 *Es ist also eine sehr simpel gehaltene Oberfläche (UI)
 * Gerade in der Software Entwicklung für mobile Endgeräte ist es wichtig, dass das Design übersichtlich und intuitiv gehalten wird
 * da es im Vergleich zu einem PC Bildschirm einfach weniger Platz bietet und die Bedienung schnell gehen muss
 *
 * Die grafischen Elemente stellen hier Objekte dar
 * in den Layout files wird festgelegt, wie sie aussehen sollen und sie erhalten eine ID
 * anhand dieser ID wird ein Objekt in den Java files erstellt und angesprochen und somit auf Interaktionen mit dem User reagiert
 * man nennt das Verbinden von UI Objekten und Java Code "viewbinding"
 *
 *
 * In diesem Projekt wurde das Viewbinding mittels einer externen Bibliothek namens "Butterknife" durchgeführt
 * Bibliotheken stellen vordefinierte Methoden zur Verfügung, die den Programmierprozess vereinfachen (etwa mathematische Berechnungen)
 * durch Butterknife wird der Code reduziert, den man für das Viewbinding braucht
 *
 * */

public class MenuActivity extends Activity {

    /**
     * Hier werden die zwei Buttons gefunden und benannt
     **/

    @BindView(R.id.btn_rbl)
    Button btn_rbl;

    @BindView(R.id.btn_stations)
    Button btn_stations;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        /** In der onCreate() Methode passieren die ersten Schritte.
         * Die Activity wird erstellt, das Layout wird gesetzt und die Buttons an die Activity gebunden */


        switch (getResources().getConfiguration().orientation) {

            case Configuration.ORIENTATION_PORTRAIT:
                setContentView(R.layout.activity_menu);
                break;

            case Configuration.ORIENTATION_LANDSCAPE:
                setContentView(R.layout.activity_menu_landscape);
                break;
        }

        ButterKnife.bind(this);


    }

    /**
     * Die MenuActivity hat nur zwei Methoden
     * Beide weisen die gleiche Funktion auf:
     * das Verhalten der App bei einem Klick auf einen Button (OnClick) wird festgelegt
     * jeder Button startet eine weitere Activity
     * der Button btn_rbl startet die Main2Activity und btn_stations startet die MainAcitvity
     */

    @OnClick(R.id.btn_rbl)
    public void searchByRbls(){

        Intent intent = new Intent(MenuActivity.this, Main2Activity.class);
        startActivity(intent);

    }

    @OnClick(R.id.btn_stations)
    public void searchByName(){

        Intent intent = new Intent(MenuActivity.this, MainActivity.class);
        startActivity(intent);

    }
}
