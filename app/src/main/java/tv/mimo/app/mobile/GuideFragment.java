package tv.mimo.app.mobile;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.*;

public class GuideFragment extends MobileBaseFragment implements MobileMainActivity.Searchable {

    private RecyclerView recyclerView;
    private GuideAdapter adapter;

    @Override
    protected int getLayoutResId() {
        return R.layout.fragment_guide;
    }

    @Override
    protected void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = findView(view, R.id.guide_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new GuideAdapter();
        recyclerView.setAdapter(adapter);

        loadGuide();
    }

    private void loadGuide() {
        // TODO: Load from EPG data via Repository
        // Placeholder for now
        adapter.setProgrammes(new java.util.ArrayList<>());
    }

    @Override
    public void onSearch(String query) {
        adapter.filter(query);
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
                String lower = query.toLowerCase();
                for (Programme p : programmes) {
                    if (p.title.toLowerCase().contains(lower) ||
                        p.channel.toLowerCase().contains(lower)) {
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
            if (programme.description != null && !programme.description.isEmpty()) {
                holder.description.setText(programme.description);
                holder.description.setVisibility(View.VISIBLE);
            } else {
                holder.description.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return filtered.isEmpty() ? 1 : filtered.size(); // Show empty state
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final TextView channel;
            final TextView title;
            final TextView time;
            final TextView description;
            final TextView empty;

            ViewHolder(View itemView) {
                super(itemView);
                channel = itemView.findViewById(R.id.programme_channel);
                title = itemView.findViewById(R.id.programme_title);
                time = itemView.findViewById(R.id.programme_time);
                description = itemView.findViewById(R.id.programme_description);
                empty = itemView.findViewById(R.id.programme_empty);
            }
        }
    }

    static class Programme {
        final String channel;
        final String title;
        final String time;
        final String description;

        Programme(String channel, String title, String time, String description) {
            this.channel = channel;
            this.title = title;
            this.time = time;
            this.description = description;
        }
    }
}