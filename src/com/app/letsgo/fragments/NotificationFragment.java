package com.app.letsgo.fragments;

import com.app.letsgo.R;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class NotificationFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      // Defines the xml file for the fragment
      View view = inflater.inflate(R.layout.fragment_notification, container, false);
      // Setup handles to view objects here
      // etFoo = (EditText) v.findViewById(R.id.etFoo);
      return view;
    }
}
