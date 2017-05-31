/**	Garbage Guru
   * @author Brandon Do
   * @version 04/11/15 ICS4UP
   * This app uses the vision api and waste wizard database to determine the disposal method of objects
   * that the user takes a picture of.
   */
package td.brandon.garbageguru;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.view.MenuItem;

public class MainActivity extends Activity implements
		CurrentImageFragment.OnFragmentInteractionListener {

	/**
	 * This method is ran on the start of the application and begins the camera intent
	 * @param savedInstanceState 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		BottomNavigationView bottomNavigationView = (BottomNavigationView)
				findViewById(R.id.navigation);

		bottomNavigationView.setOnNavigationItemSelectedListener
				(new BottomNavigationView.OnNavigationItemSelectedListener() {
					@Override
					public boolean onNavigationItemSelected(@NonNull MenuItem item) {
						Fragment selectedFragment;
						switch (item.getItemId()) {
							case R.id.action_camera:
								selectedFragment = CurrentImageFragment.newInstance();
								break;
							default:
								return false;
						}
						FragmentTransaction transaction = getFragmentManager().beginTransaction();
						transaction.replace(R.id.frame_layout, selectedFragment);
						transaction.commit();
						return true;
					}
				});

		//Manually displaying the first fragment - one time only
		FragmentTransaction transaction = getFragmentManager().beginTransaction();
		transaction.replace(R.id.frame_layout, CurrentImageFragment.newInstance());
		transaction.commit();
	}

	@Override
	public void onFragmentInteraction(Uri uri){

	}

}