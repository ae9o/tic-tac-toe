/*
 * Copyright (C) 2022 Alexei Evdokimenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ae9o.tictactoe.gui;

import ae9o.tictactoe.R;
import ae9o.tictactoe.databinding.ActivityMainBinding;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

/**
 * The main (and only) app's Activity. Manages the operation of the {@link NavController} that switches app's
 * Fragments when requested by the user. Manages the app's 3-dot menu.
 */
public class MainActivity extends AppCompatActivity {
    /** Config for the Up arrow in the toolbar. */
    private AppBarConfiguration appBarConfiguration;
    /** Controller for switching between app fragments. */
    private NavController navController;
    /** Main model with current game settings. */
    private MainViewModel viewModel;

    /**
     * Inflates the instance with views from the xml resource. Also requests from the ViewModelProvider
     * and caches the MainViewModel containing the game settings set by the user.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        // Place views.
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        // Add an Up arrow to the toolbar.
        navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
    }

    /**
     * Initializes the contents of the app's 3-dot menu.
     *
     * @param menu The menu in which items are placed.
     *
     * @return true for the menu to be displayed,
     *         false if it will not be shown.
     */
    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * This hook is called whenever an item in app's 3-dot menu is selected.
     *
     * @param item The menu item that was selected.
     *
     * @return false to allow normal menu processing to proceed,
     *         true to consume it here.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();

        // Note: since ADT 14 resource identifiers are no longer final. Google states to use "if/else" conditions
        // instead of "switch" (http://tools.android.com/tips/non-constant-fields).

        //noinspection SimplifiableIfStatement
        if (id == R.id.settings_item) {
            navController.navigate(R.id.action_global_settings);
            return true;
        } else if (id == R.id.about_item) {
            navController.navigate(R.id.action_global_about);
            return true;
        } else if (id == R.id.exit_item) {
            finishAffinity();
            return true;
        } else if (id == R.id.clear_score_item) {
            viewModel.clearScore();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Handles the Up button by delegating its behavior to the NavController.
     *
     * @return true if Up navigation completed successfully and this Activity was finished,
     *         false otherwise.
     */
    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp();
    }
}