package tv.mimo.app.mobile;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import tv.mimo.app.R;

public class MobileMainActivity extends AppCompatActivity {
    @Override protected void attachBaseContext(android.content.Context base){
        String language=new tv.mimo.app.Repository(base).language();
        android.content.res.Configuration configuration=new android.content.res.Configuration(base.getResources().getConfiguration());
        configuration.setLocale(java.util.Locale.forLanguageTag(language));
        super.attachBaseContext(base.createConfigurationContext(configuration));
    }

    private BottomNavigationView bottomNavigation;
    private SearchView searchView;
    private Fragment currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mobile_main);

        bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavigation.setOnItemSelectedListener(this::onNavigationItemSelected);

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
            bottomNavigation.setSelectedItemId(R.id.nav_home);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.mobile_search, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        searchView = (SearchView) searchItem.getActionView();
        if (searchView != null) {
            searchView.setQueryHint(getString(R.string.search_hint));
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    handleSearch(query);
                    return true;
                }
                @Override
                public boolean onQueryTextChange(String newText) {
                    handleSearch(newText);
                    return true;
                }
            });
        }
        return true;
    }

    private boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment fragment = null;
        int itemId = item.getItemId();
        if (itemId == R.id.nav_home) {
            fragment = new HomeFragment();
        } else if (itemId == R.id.nav_live_tv) {
            fragment = new LiveTvFragment();
        } else if (itemId == R.id.nav_favorites) {
            fragment = new FavoritesFragment();
        } else if (itemId == R.id.nav_guide) {
            fragment = new GuideFragment();
        } else if (itemId == R.id.nav_settings) {
            fragment = new SettingsFragment();
        }
        return loadFragment(fragment);
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
            currentFragment = fragment;
            return true;
        }
        return false;
    }

    public void openLive(String category,String query) {
        bottomNavigation.setSelectedItemId(R.id.nav_live_tv);
        if(currentFragment instanceof LiveTvFragment){android.os.Bundle args=new android.os.Bundle();args.putString("category",category);args.putString("query",query);currentFragment.setArguments(args);}
    }

    private void handleSearch(String query) {
        currentFragment=getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (currentFragment instanceof Searchable) {
            ((Searchable) currentFragment).onSearch(query);
        }
    }

    public interface Searchable {
        void onSearch(String query);
    }
}

