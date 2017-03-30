package co.jijichat;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import co.jijichat.muc.MucFragment;

public class MyFragmentPagerAdapter extends FragmentPagerAdapter {
	final int PAGE_COUNT = 1;

	public MyFragmentPagerAdapter(FragmentManager fm) {
		super(fm);
	}

	@Override
	public Fragment getItem(int arg0) {
		switch (arg0) {
		case 0:
			return MucFragment.newInstance();
//		case 1:
//			return ChatListFragment.newInstance();
//		case 2:
//			return RosterFragment.newInstance();
		}
		return null;
	}

	@Override
	public int getCount() {
		return PAGE_COUNT;
	}
}
