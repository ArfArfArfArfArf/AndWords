package com.roadkill.andwords;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

public class Preferences extends PreferenceActivity
{

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.layout.preferences);
		
		PreferenceScreen s = getPreferenceScreen();
		
		CheckBoxPreference cp = (CheckBoxPreference) s.findPreference(getString(R.string.enable_NYT_Daily));
		
		if (!cp.isChecked())
		{
			EditTextPreference u = (EditTextPreference) s.findPreference(getString(R.string.NYT_Username));
			u.setEnabled(false);
			EditTextPreference p = (EditTextPreference) s.findPreference(getString(R.string.NYT_Password));
			p.setEnabled(false);
		}
		
		cp.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
		{
			public boolean onPreferenceChange(Preference p, Object newValue)
			{
				PreferenceScreen s = getPreferenceScreen();
				boolean state = false;
				
				if (newValue.toString().equals("true"))
				{
					state = true;
				}

				EditTextPreference username = (EditTextPreference) s.findPreference(getString(R.string.NYT_Username));
				username.setEnabled(state);
				EditTextPreference password = (EditTextPreference) s.findPreference(getString(R.string.NYT_Password));
				password.setEnabled(state);
				return true;
			}
		});
		
		ListPreference p = (ListPreference) s.findPreference(getString(R.string.screen_orientation));
		p.setSummary(p.getEntry());
		
		p.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() 
		{
			public boolean onPreferenceChange(Preference preference, Object newValue) 
			{
				Integer i = Integer.parseInt(newValue.toString());
				String[] strLayouts = getResources().getStringArray(R.array.screen_orientation_array);
				
				preference.setSummary(strLayouts[i]);
				return true;
			}
		});
		
		p = (ListPreference) s.findPreference(getString(R.string.screen_theme));
		p.setSummary(p.getEntry());
		
		p.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() 
		{
			public boolean onPreferenceChange(Preference preference, Object newValue) 
			{
				Integer i = Integer.parseInt(newValue.toString());
				String[] strLayouts = getResources().getStringArray(R.array.screen_theme_array);
				
				preference.setSummary(strLayouts[i]);
				return true;
			}
		});
		
		p = (ListPreference)s.findPreference(getString(R.string.end_of_word));
		p.setSummary(p.getEntry());
		
		p.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() 
		{
			public boolean onPreferenceChange(Preference preference, Object newValue) 
			{
				preference.setSummary(newValue.toString());
				return true;
			}
		});
		
		p = (ListPreference)s.findPreference(getString(R.string.clue_font_size));
		p.setSummary(p.getEntry());
		
		p.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() 
		{
			public boolean onPreferenceChange(Preference preference, Object newValue) 
			{
				String strVal = newValue.toString();
				
				if (strVal.equals("60.0"))
					preference.setSummary("Extra Large");
				else if (strVal.equals("45.0"))
					preference.setSummary("Large");
				else if (strVal.equals("30.0"))
					preference.setSummary("Medium");
				else if (strVal.equals("15.0"))
					preference.setSummary("Small");
					
				return true;
			}
		});
		
		p = (ListPreference)s.findPreference(getString(R.string.volume_rocker));
		
		if (p.getEntry() == null)
			p.setSummary("Word");
		else
			p.setSummary(p.getEntry());
		
		p.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() 
		{
			public boolean onPreferenceChange(Preference preference, Object newValue) 
			{
				String strVal = newValue.toString();
				
				preference.setSummary(strVal);
					
				return true;
			}
		});
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String screenLayout = prefs.getString(getString(R.string.screen_orientation), getString(R.string.screen_orientation_default_value));
		
		// force the screen layout if requested to do so
		if (screenLayout.equals("1"))
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		else if (screenLayout.equals("2"))
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		else if (screenLayout.equals("0"))
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
		
	}
}
