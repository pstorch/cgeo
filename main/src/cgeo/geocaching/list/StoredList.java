package cgeo.geocaching.list;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.functions.Action1;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ListView;

import java.lang.ref.WeakReference;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

public final class StoredList extends AbstractList {
    private static final int TEMPORARY_LIST_ID = 0;
    public static final StoredList TEMPORARY_LIST = new StoredList(TEMPORARY_LIST_ID, "<temporary>", 0); // Never displayed
    public static final int STANDARD_LIST_ID = 1;
    private final int count; // this value is only valid as long as the list is not changed by other database operations

    public StoredList(final int id, final String title, final int count) {
        super(id, title);
        this.count = count;
    }

    @Override
    public String getTitleAndCount() {
        return title + " [" + count + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof StoredList)) {
            return false;
        }
        return id == ((StoredList) obj).id;
    }

    public static class UserInterface {
        private final WeakReference<Activity> activityRef;
        private final Application app;
        private final Resources res;

        public UserInterface(@NonNull final Activity activity) {
            this.activityRef = new WeakReference<>(activity);
            app = CgeoApplication.getInstance();
            res = app.getResources();
        }

        public void promptForListSelection(final int titleId, @NonNull final Action1<Integer> runAfterwards, final boolean onlyConcreteLists, final int exceptListId) {
            promptForListSelection(titleId, runAfterwards, onlyConcreteLists, Collections.singleton(exceptListId), ListNameMemento.EMPTY);
        }

        public void promptForMultiListSelection(final int titleId, @NonNull final Action1<Set<Integer>> runAfterwards, final boolean onlyConcreteLists, final Set<Integer> currentListIds, final boolean fastStoreOnLastSelection) {
            promptForMultiListSelection(titleId, runAfterwards, onlyConcreteLists, Collections.<Integer>emptySet(), currentListIds, ListNameMemento.EMPTY, fastStoreOnLastSelection);
        }

        public void promptForListSelection(final int titleId, @NonNull final Action1<Integer> runAfterwards, final boolean onlyConcreteLists, final int exceptListId, @NonNull final ListNameMemento listNameMemento) {
            promptForListSelection(titleId, runAfterwards, onlyConcreteLists, Collections.singleton(exceptListId), listNameMemento);
        }

        public void promptForMultiListSelection(final int titleId, @NonNull final Action1<Set<Integer>> runAfterwards, final boolean onlyConcreteLists, final Set<Integer> exceptListIds, final Set<Integer> currentListIds, @NonNull final ListNameMemento listNameMemento, final boolean fastStoreOnLastSelection) {
            final Set<Integer> selectedListIds = new HashSet<>(fastStoreOnLastSelection ? Settings.getLastSelectedLists() : currentListIds);
            final List<AbstractList> lists = getMenuLists(onlyConcreteLists, exceptListIds, selectedListIds);

            final CharSequence[] listTitles = new CharSequence[lists.size()];
            final boolean[] selectedItems = new boolean[lists.size()];
            final Set<Integer> allListIds = new HashSet<>(lists.size());
            for (int i = 0 ; i < lists.size() ; i++) {
                final AbstractList list = lists.get(i);
                listTitles[i] = list.getTitleAndCount();
                selectedItems[i] = selectedListIds.contains(list.id);
                allListIds.add(list.id);
            }
            // remove from selected which are not available anymore
            selectedListIds.retainAll(allListIds);

            if (fastStoreOnLastSelection && !selectedListIds.isEmpty()) {
                runAfterwards.call(selectedListIds);
                return;
            }

            final Activity activity = activityRef.get();
            final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(res.getString(titleId));
            builder.setMultiChoiceItems(listTitles, selectedItems, new MultiChoiceClickListener(lists, selectedListIds));
            builder.setPositiveButton(android.R.string.ok, new OnOkClickListener(selectedListIds, runAfterwards, listNameMemento));
            final Set<Integer> lastSelectedLists = Settings.getLastSelectedLists();
            if (currentListIds.isEmpty() && !lastSelectedLists.isEmpty()) {
                // onClickListener is null because it is handled below to prevent closing of the dialog
                builder.setNeutralButton(R.string.cache_list_select_last, null);
            }
            builder.setNegativeButton(android.R.string.cancel, null);
            final AlertDialog dialog = builder.create();
            dialog.show();
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(!selectedListIds.isEmpty());
            dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener(new OnLastSelectionClickListener(selectedListIds, lastSelectedLists, dialog, lists, selectedItems));
        }

        public void promptForListSelection(final int titleId, @NonNull final Action1<Integer> runAfterwards, final boolean onlyConcreteLists, final Set<Integer> exceptListIds, @NonNull final ListNameMemento listNameMemento) {
            final List<AbstractList> lists = getMenuLists(onlyConcreteLists, exceptListIds, Collections.<Integer>emptySet());

            final List<CharSequence> listsTitle = new ArrayList<>();
            for (final AbstractList list : lists) {
                listsTitle.add(list.getTitleAndCount());
            }

            final CharSequence[] items = new CharSequence[listsTitle.size()];

            final Activity activity = activityRef.get();
            final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(res.getString(titleId));
            builder.setItems(listsTitle.toArray(items), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(final DialogInterface dialogInterface, final int itemId) {
                    final AbstractList list = lists.get(itemId);
                    if (list == PseudoList.NEW_LIST) {
                        // create new list on the fly
                        promptForListCreation(runAfterwards, listNameMemento.getTerm());
                    } else {
                        runAfterwards.call(lists.get(itemId).id);
                    }
                }
            });
            builder.create().show();
        }

        public static List<AbstractList> getMenuLists(final boolean onlyConcreteLists, final int exceptListId) {
            return getMenuLists(onlyConcreteLists, Collections.singleton(exceptListId), Collections.<Integer>emptySet());
        }

        public static List<AbstractList> getMenuLists(final boolean onlyConcreteLists, final Set<Integer> exceptListIds, final Set<Integer> selectedLists) {
            final List<AbstractList> lists = new ArrayList<AbstractList>(getSortedLists(selectedLists));

            if (exceptListIds.contains(STANDARD_LIST_ID)) {
                lists.remove(DataStore.getList(STANDARD_LIST_ID));
            }

            for (final Integer exceptListId : exceptListIds) {
                if (exceptListId > 0) {
                    lists.remove(DataStore.getList(exceptListId));
                }
            }

            if (!onlyConcreteLists) {
                if (!exceptListIds.contains(PseudoList.ALL_LIST.id)) {
                    lists.add(PseudoList.ALL_LIST);
                }
                if (!exceptListIds.contains(PseudoList.HISTORY_LIST.id)) {
                    lists.add(PseudoList.HISTORY_LIST);
                }
            }
            if (!exceptListIds.contains(PseudoList.NEW_LIST.id)) {
                lists.add(PseudoList.NEW_LIST);
            }
            return lists;
        }

        @NonNull
        private static List<StoredList> getSortedLists(final Set<Integer> selectedLists) {
            final Collator collator = Collator.getInstance();
            final List<StoredList> lists = DataStore.getLists();
            Collections.sort(lists, new Comparator<StoredList>() {

                @Override
                public int compare(final StoredList lhs, final StoredList rhs) {
                    if (selectedLists.contains(lhs.id) && !selectedLists.contains(rhs.id)) {
                        return -1;
                    }
                    if (selectedLists.contains(rhs.id) && !selectedLists.contains(lhs.id)) {
                        return 1;
                    }
                    // have the standard list at the top
                    if (lhs.id == STANDARD_LIST_ID) {
                        return -1;
                    }
                    if (rhs.id == STANDARD_LIST_ID) {
                        return 1;
                    }
                    // otherwise sort alphabetical
                    return collator.compare(lhs.getTitle(), rhs.getTitle());
                }
            });
            return lists;
        }

        public void promptForListCreation(@NonNull final Action1<Integer> runAfterwards, final String newListName) {
            handleListNameInput(newListName, R.string.list_dialog_create_title, R.string.list_dialog_create, new Action1<String>() {

                // We need to update the list cache by creating a new StoredList object here.
                @SuppressWarnings("unused")
                @Override
                public void call(final String listName) {
                    final Activity activity = activityRef.get();
                    if (activity == null) {
                        return;
                    }
                    final int newId = DataStore.createList(listName);

                    if (newId > 0) {
                        new StoredList(newId, listName, 0);
                        runAfterwards.call(newId);
                    } else {
                        ActivityMixin.showToast(activity, res.getString(R.string.list_dialog_create_err));
                    }
                }
            });
        }

        public void promptForListCreation(@NonNull final Action1<Set<Integer>> runAfterwards, final Set<Integer> selectedLists, final String newListName) {
            handleListNameInput(newListName, R.string.list_dialog_create_title, R.string.list_dialog_create, new Action1<String>() {

                // We need to update the list cache by creating a new StoredList object here.
                @SuppressWarnings("unused")
                @Override
                public void call(final String listName) {
                    final Activity activity = activityRef.get();
                    if (activity == null) {
                        return;
                    }
                    final int newId = DataStore.createList(listName);

                    if (newId > 0) {
                        new StoredList(newId, listName, 0);
                        selectedLists.remove(PseudoList.NEW_LIST.id);
                        selectedLists.add(newId);
                        Settings.setLastSelectedLists(selectedLists);
                        runAfterwards.call(selectedLists);
                    } else {
                        ActivityMixin.showToast(activity, res.getString(R.string.list_dialog_create_err));
                    }
                }
            });
        }

        private void handleListNameInput(final String defaultValue, final int dialogTitle, final int buttonTitle, final Action1<String> runnable) {
            final Activity activity = activityRef.get();
            if (activity == null) {
                return;
            }
            Dialogs.input(activity, dialogTitle, defaultValue, buttonTitle, new Action1<String>() {

                @Override
                public void call(final String input) {
                    // remove whitespaces added by autocompletion of Android keyboard
                    final String listName = StringUtils.trim(input);
                    if (StringUtils.isNotBlank(listName)) {
                        runnable.call(listName);
                    }
                }
            });
        }

        public void promptForListRename(final int listId, @NonNull final Runnable runAfterRename) {
            final StoredList list = DataStore.getList(listId);
            handleListNameInput(list.title, R.string.list_dialog_rename_title, R.string.list_dialog_rename, new Action1<String>() {

                @Override
                public void call(final String listName) {
                    DataStore.renameList(listId, listName);
                    runAfterRename.run();
                }
            });
        }

        private static class MultiChoiceClickListener implements DialogInterface.OnMultiChoiceClickListener {
            private final List<AbstractList> lists;
            private final Set<Integer> selectedListIds;

            public MultiChoiceClickListener(final List<AbstractList> lists, final Set<Integer> selectedListIds) {
                this.lists = lists;
                this.selectedListIds = selectedListIds;
            }

            @Override
            public void onClick(final DialogInterface dialog, final int itemId, final boolean isChecked) {
                final AbstractList list = lists.get(itemId);
                if (isChecked) {
                    selectedListIds.add(list.id);
                } else {
                    selectedListIds.remove(list.id);
                }
                ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(!selectedListIds.isEmpty());
            }
        }

        private static class OnLastSelectionClickListener implements View.OnClickListener {
            private final Set<Integer> selectedListIds;
            private final Set<Integer> lastSelectedLists;
            private final AlertDialog dialog;
            private final List<AbstractList> lists;
            private final boolean[] selectedItems;

            public OnLastSelectionClickListener(final Set<Integer> selectedListIds, final Set<Integer> lastSelectedLists, final AlertDialog dialog, final List<AbstractList> lists, final boolean[] selectedItems) {
                this.selectedListIds = selectedListIds;
                this.lastSelectedLists = lastSelectedLists;
                this.dialog = dialog;
                this.lists = lists;
                this.selectedItems = selectedItems;
            }

            @Override
            public void onClick(final View v) {
                selectedListIds.clear();
                selectedListIds.addAll(lastSelectedLists);
                final ListView listView = dialog.getListView();
                for (int i = 0; i < lists.size() ; i++) {
                    selectedItems[i] = selectedListIds.contains(lists.get(i).id);
                    listView.setItemChecked(i, selectedItems[i]);
                }
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(!selectedListIds.isEmpty());
            }
        }

        private class OnOkClickListener implements DialogInterface.OnClickListener {
            private final Set<Integer> selectedListIds;
            private final Action1<Set<Integer>> runAfterwards;
            private final ListNameMemento listNameMemento;

            public OnOkClickListener(final Set<Integer> selectedListIds, final Action1<Set<Integer>> runAfterwards, final ListNameMemento listNameMemento) {
                this.selectedListIds = selectedListIds;
                this.runAfterwards = runAfterwards;
                this.listNameMemento = listNameMemento;
            }

            @Override
            public void onClick(final DialogInterface dialog, final int id) {
                if (selectedListIds.contains(PseudoList.NEW_LIST.id)) {
                    // create new list on the fly
                    promptForListCreation(runAfterwards, selectedListIds, listNameMemento.getTerm());
                } else {
                    Settings.setLastSelectedLists(selectedListIds);
                    runAfterwards.call(selectedListIds);
                }
                dialog.cancel();
            }
        }
    }

    /**
     * Get the list title.
     */
    @Override
    @NonNull
    public String getTitle() {
        return title;
    }

    @Override
    public int getNumberOfCaches() {
        return count;
    }

    /**
     * Return the given list, if it is a concrete list. Return the default list otherwise.
     */
    public static int getConcreteList(final int listId) {
        if (listId == PseudoList.ALL_LIST.id || listId == TEMPORARY_LIST.id || listId == PseudoList.HISTORY_LIST.id) {
            return STANDARD_LIST_ID;
        }
        return listId;
    }

    @Override
    public boolean isConcrete() {
        return true;
    }

}
