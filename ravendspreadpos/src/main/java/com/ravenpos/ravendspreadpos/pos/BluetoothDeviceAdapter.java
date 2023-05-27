package com.ravenpos.ravendspreadpos.pos;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ravenpos.ravendspreadpos.R;
import com.ravenpos.ravendspreadpos.model.BluetoothModel;
import com.ravenpos.ravendspreadpos.utils.AppLog;

import java.util.List;

public class BluetoothDeviceAdapter extends RecyclerView.Adapter<BluetoothDeviceAdapter.ViewHolder> {

    private Context context;
    private List<BluetoothModel> accountPrivilege;
    private LayoutInflater inflater;
    private IBluetooth callback;

    private int mCheckedPosition = -1;

    public BluetoothDeviceAdapter(Context context, IBluetooth callbackV) {
        inflater = LayoutInflater.from(context);
        this.context = context;
        this.callback = callbackV;
    }

    public void swapData(List<BluetoothModel> productEntities) {
        this.accountPrivilege = productEntities;
        notifyDataSetChanged();
    }

    public void updateAdapterPosition(){
        mCheckedPosition = -1;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BluetoothDeviceAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.device_list_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull BluetoothDeviceAdapter.ViewHolder holder, int position) {
        final BluetoothModel productModel = accountPrivilege.get(position);
        try {
            holder.account.setText(productModel.title);
            holder.accountName.setOnCheckedChangeListener(null);
            holder.accountName.setChecked(position == mCheckedPosition);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mCheckedPosition = position;
                    holder.accountName.setChecked(true);
                        callback.getSelectedDevice(productModel);
                }
            });
        }catch (Exception e){
            AppLog.e("onBindViewHolder",e.getLocalizedMessage());
        }
    }

    @Override
    public int getItemCount() {
        return accountPrivilege != null ? accountPrivilege.size() : 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public RadioButton accountName;
        public TextView account;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            accountName = itemView.findViewById(R.id.country_radio);
            account = itemView.findViewById(R.id.txt_country);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {

        }
    }
}
