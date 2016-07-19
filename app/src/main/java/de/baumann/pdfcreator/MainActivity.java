package de.baumann.pdfcreator;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.baumann.pdfcreator.pages.add_text;
import de.baumann.pdfcreator.pages.create_image;
import de.baumann.pdfcreator.pages.add_image;
import de.baumann.pdfcreator.pages.create_text;

public class MainActivity extends AppCompatActivity {

    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;

    private final Date date = new Date();
    private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        }

        // Get intent, action and MIME type
        Intent intent = this.getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean appStarted = sharedPref.getBoolean("appStarted", true);

        if (appStarted) {
            if (Intent.ACTION_SEND.equals(action) && type != null) {
                if (type.startsWith("image/")) {
                    sharedPref.edit()
                            .putInt("startFragment", 0)
                            .apply(); // Handle single image being sent
                } else if (type.startsWith("text/")) {
                    sharedPref.edit()
                            .putInt("startFragment", 1)
                            .apply(); // Handle single image being sent
                }
            }
        }

        ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        setupViewPager(viewPager);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        assert tabLayout != null;
        tabLayout.setupWithViewPager(viewPager);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (android.os.Build.VERSION.SDK_INT >= 23) {
            int hasWRITE_EXTERNAL_STORAGE = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (hasWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
                if (!shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setMessage(R.string.app_permissions)
                            .setPositiveButton(getString(R.string.toast_yes), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (android.os.Build.VERSION.SDK_INT >= 23)
                                        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                                REQUEST_CODE_ASK_PERMISSIONS);
                                }
                            })
                            .setNegativeButton(getString(R.string.toast_cancel), null)
                            .show();
                    return;
                }
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_CODE_ASK_PERMISSIONS);
            }
        }

        File directory = new File(Environment.getExternalStorageDirectory() + "/Android/data/de.baumann.pdf/");
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    private void setupViewPager(ViewPager viewPager) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        int startFragment = sharedPref.getInt("startFragment", 0);

        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());

        adapter.addFragment(new create_image(), String.valueOf(getString(R.string.create_image)));
        adapter.addFragment(new create_text(), String.valueOf(getString(R.string.create_text)));
        adapter.addFragment(new add_image(), String.valueOf(getString(R.string.add_image)));
        adapter.addFragment(new add_text(), String.valueOf(getString(R.string.add_text)));

        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(startFragment);
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);// add return null; to display only icons
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        String title;
        if (id == R.id.action_share) {

            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            title = sharedPref.getString("title", null);
            String path = sharedPref.getString("pathPDF", Environment.getExternalStorageDirectory() +
                    "/Android/data/de.baumann.pdf/" + dateFormat.format(date) + "_" + title + ".pdf");
            String FileTitle = path.substring(path.lastIndexOf("/")+1);

            Uri myUri= Uri.fromFile(new File(path));
            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
            sharingIntent.setType("application/pdf");
            sharingIntent.putExtra(Intent.EXTRA_STREAM, myUri);
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, FileTitle);
            sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(sharingIntent, getString(R.string.action_share_with)));
        }

        if (id == R.id.action_open) {

            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            title = sharedPref.getString("title", null);
            String path = sharedPref.getString("pathPDF", Environment.getExternalStorageDirectory() +
                    "/Android/data/de.baumann.pdf/" + dateFormat.format(date) + "_" + title + ".pdf");

            File file = new File(path);
            Intent target = new Intent(Intent.ACTION_VIEW);
            target.setDataAndType(Uri.fromFile(file),"application/pdf");
            target.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

            try {
                startActivity(target);
            } catch (ActivityNotFoundException e) {
                // Instruct the user to install a PDF reader here, or something
                LayoutInflater inflater = getLayoutInflater();

                View toastLayout = inflater.inflate(R.layout.toast,
                        (ViewGroup) findViewById(R.id.toast_root_view));

                TextView header = (TextView) toastLayout.findViewById(R.id.toast_message);
                header.setText(R.string.toast_install_pdf);

                Toast toast = new Toast(getApplicationContext());
                toast.setGravity(Gravity.FILL_HORIZONTAL | Gravity.BOTTOM, 0, 0);
                toast.setDuration(Toast.LENGTH_LONG);
                toast.setView(toastLayout);
                toast.show();
            }

        }

        if (id == R.id.action_folder) {

            File directory = new File(Environment.getExternalStorageDirectory() + "/Android/data/de.baumann.pdf/");

            Intent target = new Intent(Intent.ACTION_VIEW);
            target.setDataAndType(Uri.fromFile(directory), "resource/folder");

            try {
                startActivity (target);
            } catch (ActivityNotFoundException e) {
                LayoutInflater inflater = getLayoutInflater();

                View toastLayout = inflater.inflate(R.layout.toast,
                        (ViewGroup) findViewById(R.id.toast_root_view));

                TextView header = (TextView) toastLayout.findViewById(R.id.toast_message);
                header.setText(R.string.toast_install_folder);

                Toast toast = new Toast(getApplicationContext());
                toast.setGravity(Gravity.FILL_HORIZONTAL | Gravity.BOTTOM, 0, 0);
                toast.setDuration(Toast.LENGTH_LONG);
                toast.setView(toastLayout);
                toast.show();
            }
        }

        if (id == R.id.action_settings) {

            Intent intent = new Intent(this, UserSettingsActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPref.edit()
                .putInt("startFragment", 0)
                .putBoolean("appStarted", true)
                .apply();
        super.onBackPressed();
    }
}