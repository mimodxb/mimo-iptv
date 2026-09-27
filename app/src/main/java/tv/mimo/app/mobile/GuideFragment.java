package tv.mimo.app.mobile;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.*;
import tv.mimo.app.R;
import tv.mimo.app.Repository;
import tv.mimo.app.Channel;
import tv.mimo.app.Epg;
import java.util.*;

public class GuideFragment extends MobileBaseFragment implements MobileMainActivity.Searchable {

    private RecyclerView recyclerView;
    private TextView emptyView;
    private GuideAdapter adapter;

    @Override
    protected int getLayoutResId() {
        return R.layout.fragment_guide;
    }

    @Override
    public void onViewReady(@NonNull View view, @Nullable Bundle savedInstanceState) {
        recyclerView = findView(view, R.id.guide_recycler);
        emptyView = findView(view, R.id.guide_empty);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new GuideAdapter();
        recyclerView.setAdapter(adapter);

        loadGuide();
    }

    private void loadGuide() {
        emptyView.setText(R.string.loading);
        emptyView.setVisibility(View.VISIBLE);
        MobileCatalog.load(requireContext(),false,catalog->{
            if(getView()==null)return;
            new Thread(()->{
                List<Programme> rows=new ArrayList<>();
                Set<String> ids=new HashSet<>();
                for(Channel channel:catalog.channels)if(!channel.tvgId.isEmpty())ids.add(channel.tvgId);
                Map<String,List<Epg.Programme>> guide=new HashMap<>();
                for(String url:catalog.epgUrls)try{
                    Map<String,List<Epg.Programme>> parsed=Epg.fetch(url,ids,System.currentTimeMillis());
                    for(Map.Entry<String,List<Epg.Programme>> entry:parsed.entrySet())guide.putIfAbsent(entry.getKey(),entry.getValue());
                }catch(Exception ignored){}
                java.text.DateFormat time=java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT);
                for(Channel channel:catalog.channels){
                    List<Epg.Programme> programmes=guide.get(channel.tvgId);
                    if(programmes==null)continue;
                    for(Epg.Programme p:programmes){
                        rows.add(new Programme(channel.name,p.title,time.format(new Date(p.start))+" – "+time.format(new Date(p.stop)),"",channel));
                    }
                }
                new android.os.Handler(android.os.Looper.getMainLooper()).post(()->{
                    if(getView()==null)return;
                    adapter.setProgrammes(rows);emptyView.setText(R.string.guide_unavailable);
                    emptyView.setOnClickListener(v->loadGuide());updateEmptyState();
                });
            },"MIMO-guide").start();
        });
    }

    private void updateEmptyState() {
        if (adapter.getItemCount() == 0) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onSearch(String query) {
        adapter.filter(query);
        updateEmptyState();
    }

    static class GuideAdapter extends RecyclerView.Adapter<GuideAdapter.ViewHolder> {
        private List<Programme> programmes = new ArrayList<>();
        private List<Programme> filtered = new ArrayList<>();

        void setProgrammes(List<Programme> programmes) {
            this.programmes = programmes;
            this.filtered = new ArrayList<>(programmes);
            notifyDataSetChanged();
        }

        void filter(String query) {
            filtered.clear();
            if (query == null || query.trim().isEmpty()) {
                filtered.addAll(programmes);
            } else {
                String lower = query.toLowerCase(java.util.Locale.ROOT);
                for (Programme p : programmes) {
                    if (p.title.toLowerCase(java.util.Locale.ROOT).contains(lower) ||
                        p.channel.toLowerCase(java.util.Locale.ROOT).contains(lower)) {
                        filtered.add(p);
                    }
                }
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_guide_programme, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Programme programme = filtered.get(position);
            holder.channel.setText(programme.channel);
            holder.title.setText(programme.title);
            holder.time.setText(programme.time);
            holder.itemView.setOnClickListener(v->MobileNavigation.play(v.getContext(),programme.target,Collections.singletonList(programme.target)));
            if (programme.description != null && !programme.description.isEmpty()) {
                holder.description.setText(programme.description);
                holder.description.setVisibility(View.VISIBLE);
            } else {
                holder.description.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return filtered.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final TextView channel;
            final TextView title;
            final TextView time;
            final TextView description;

            ViewHolder(View itemView) {
                super(itemView);
                channel = itemView.findViewById(R.id.programme_channel);
                title = itemView.findViewById(R.id.programme_title);
                time = itemView.findViewById(R.id.programme_time);
                description = itemView.findViewById(R.id.programme_description);
            }
        }
    }

    static class Programme {
        final String channel;
        final String title;
        final String time;
        final String description;
        final Channel target;

        Programme(String channel, String title, String time, String description, Channel target) {
            this.target=target;
            this.channel = channel;
            this.title = title;
            this.time = time;
            this.description = description;
        }
    }
}


