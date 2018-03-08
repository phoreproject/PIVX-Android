package io.phore.android.ui.settings_rates;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.phorej.core.Peer;

import java.util.List;

import io.phore.android.PhoreApplication;
import io.phore.android.R;
import io.phore.android.rate.db.PhoreRate;
import io.phore.android.ui.base.BaseRecyclerFragment;
import io.phore.android.ui.base.tools.adapter.BaseRecyclerAdapter;
import io.phore.android.ui.base.tools.adapter.BaseRecyclerViewHolder;
import io.phore.android.ui.base.tools.adapter.ListItemListeners;
import io.phore.android.ui.settings_network_activity.NetworkViewHolder;

/**
 * Created by furszy on 7/2/17.
 */

public class RatesFragment extends BaseRecyclerFragment<PhoreRate> implements ListItemListeners<PhoreRate> {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        setEmptyText("No rate available");
        setEmptyTextColor(Color.parseColor("#cccccc"));
        return view;
    }

    @Override
    protected List<PhoreRate> onLoading() {
        return phoreModule.listRates();
    }

    @Override
    protected BaseRecyclerAdapter<PhoreRate, ? extends PhoreRateHolder> initAdapter() {
        BaseRecyclerAdapter<PhoreRate, PhoreRateHolder> adapter = new BaseRecyclerAdapter<PhoreRate, PhoreRateHolder>(getActivity()) {
            @Override
            protected PhoreRateHolder createHolder(View itemView, int type) {
                return new PhoreRateHolder(itemView,type);
            }

            @Override
            protected int getCardViewResource(int type) {
                return R.layout.rate_row;
            }

            @Override
            protected void bindHolder(PhoreRateHolder holder, PhoreRate data, int position) {
                holder.txt_name.setText(data.getCode());
                if (list.get(0).getCode().equals(data.getCode()))
                    holder.view_line.setVisibility(View.GONE);
            }
        };
        adapter.setListEventListener(this);
        return adapter;
    }

    @Override
    public void onItemClickListener(PhoreRate data, int position) {
        phoreApplication.getAppConf().setSelectedRateCoin(data.getCode());
        Toast.makeText(getActivity(),R.string.rate_selected,Toast.LENGTH_SHORT).show();
        getActivity().onBackPressed();
    }

    @Override
    public void onLongItemClickListener(PhoreRate data, int position) {

    }

    private  class PhoreRateHolder extends BaseRecyclerViewHolder{

        private TextView txt_name;
        private View view_line;

        protected PhoreRateHolder(View itemView, int holderType) {
            super(itemView, holderType);
            txt_name = (TextView) itemView.findViewById(R.id.txt_name);
            view_line = itemView.findViewById(R.id.view_line);
        }
    }
}
