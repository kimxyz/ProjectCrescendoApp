package com.projectcrescendo.projectcrescendo;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ListView;

import com.projectcrescendo.projectcrescendo.models.ConcreteNote;
import com.projectcrescendo.projectcrescendo.models.Intonation;
import com.projectcrescendo.projectcrescendo.models.Note;

import java.util.ArrayList;
import java.util.List;

/**
 * An interface to allow the AddNoteFragment to communicate changes from its UI (such as note
 * additions/removals and intonation changes) back to the TutorialActivity instance that presented
 * the fragment UI.
 * <p/>
 * Created by Dylan McKee on 22/02/2016.
 */
interface AddNoteFragmentListener {
    /**
     * Called by the AddNoteFragment when a note is added to the stave by the user. This method
     * needs to add the note to the current beat that they're editing, and refresh the grid UI so
     * that the note shows up graphically to the user.
     *
     * @param addNoteFragment the fragment instance that the note was added from.
     * @param note            the note that was added by the user.
     */
    void addNoteFragmentAddedNote(AddNoteFragment addNoteFragment, Note note);

    /**
     * Called by the AddNoteFragment when a note is removed from the stave by the user. This method
     * needs to remove the note to the current beat that they're editing, and refresh the grid UI so
     * that the note removal shows up graphically to the user.
     *
     * @param addNoteFragment the fragment instance that the note was removed from.
     * @param note            the note that was removed by the user.
     */
    void addNoteFragmentDeletedNote(AddNoteFragment addNoteFragment, Note note);

    /**
     * Called by the AddNoteFragment when the intonation for the currently selected beat is changed.
     *
     * @param addNoteFragment the fragment instance that the intonation was edited from.
     * @param newIntonation   the new intonation for the beat.
     */
    void addNoteFragmentIntonationSelected(AddNoteFragment addNoteFragment, Intonation newIntonation);
}

/**
 * A fragment UI to allow users to add and remove notes from the current beat, and edit the
 * intonation of the current beat. Presented from the TutorialActivity, this class uses the
 * AddNoteFragmentListener interface (defined above) to give callbacks to the TutorialActivity, so
 * that the activity can update the Stave model.
 * <p/>
 * This design pattern allows us to adhere to the Model-View-Controller (MVC) design pattern.
 * <p/>
 * Created by Dylan McKee on 22/02/2016.
 */
public class AddNoteFragment extends DialogFragment implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener, SelectIntonationFragmentCallbackListener {

    /**
     * A class-wide note placeholder to hold the Note instance being added in the current fragment.
     */
    private Note noteToAdd = null;
    /**
     * The intonation of the current beat being edited in this fragment.
     */
    private Intonation currentIntonation;
    /**
     * A list of Notes for the current Beat instance being edited by this fragment.
     */
    private List<Note> notesForCurrentBeat;
    /**
     * A list view to present the list of notes for the current beat in so that they can be edited.
     */
    private ListView listView;

    /**
     * The 'select intonation' button.
     */
    private Button intonationButton;

    /**
     * A reference to the AddNoteFragmentListener, so that callback events can be issued.
     */
    private AddNoteFragmentListener addNoteFragmentListener;

    /**
     * A blank constructor.
     */
    public AddNoteFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment AddNoteFragment.
     */
    public static AddNoteFragment newInstance(Intonation intonation, List<Note> notesForCurrentBar) {
        AddNoteFragment fragment = new AddNoteFragment();

        fragment.currentIntonation = intonation;
        fragment.notesForCurrentBeat = notesForCurrentBar;

        return fragment;
    }


    /**
     * When the fragment is created, this method ensures that the UI elements are instantiated from
     * the XML and refreshes the notes list and intonation initially.
     *
     * @param savedInstanceState the saved state of the fragment, if any exists.
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        listView = (ListView) this.getView().findViewById(R.id.noteListView);

        listView.setOnItemClickListener(this);
        listView.setOnItemLongClickListener(this);

        intonationButton = (Button) this.getView().findViewById(R.id.intonationSelectionButton);
        intonationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectIntonation();
            }
        });

        if (notesForCurrentBeat == null) {
            notesForCurrentBeat = new ArrayList<Note>();
        }

        refreshNotesList();

        refreshIntonation();


    }


    /**
     * This method presents the 'Select Intonation Fragment' when the 'intonation' button is tapped.
     */
    private void selectIntonation() {
        // Present a modal that allows the user to select intonation...

        SelectIntonationFragment selectIntonationFragment = new SelectIntonationFragment();

        selectIntonationFragment.setListener(this);

        selectIntonationFragment.show(getActivity().getFragmentManager(), getString(R.string.add_intonation_title));

    }

    /**
     * Sets the intonation button text to whatever the currently selected intonation's name is.
     */
    private void refreshIntonation() {
        intonationButton.setText(String.format("%s%s", getString(R.string.expression), getCurrentIntonation()));
    }

    /**
     * Refreshes the list of notes within the current fragment; to be called on load, and after
     * a note has been added/deleted.
     */
    private void refreshNotesList() {
        // Refresh the Android ListView...

        List<String> noteTitleList = new ArrayList<String>();

        // Add note names to the list...
        for (Note note : getNotesForCurrentBeat()) {
            Log.d("AddNoteFragment", "note = " + note.toString());

            String title = String.format(getString(R.string.add_note_length), note.getPitch(), note.getLength());

            noteTitleList.add(title);

        }

        // If there's less than 5 notes in the bar; add the option to add a note too...
        if (getNotesForCurrentBeat().size() < 5) {
            noteTitleList.add(getString(R.string.add_note_button_text));
        }

        ArrayAdapter<String> listViewAdapter = new ArrayAdapter<String>(this.getActivity(), R.layout.note_list_row, noteTitleList);

        // refresh using the list view adapter
        listView.setAdapter(listViewAdapter);

    }


    /**
     * Presents the user with the 'add note' UI flow, where they are asked to enter some details
     * about their desired new note, which are then validated against stored names in the database,
     * and if valid, get added to the current Beat.
     */
    private void addNewNote() {
        // Get all possible note names so we have something to validate against...
        final List<String> validNoteNames = new NoteManager(getActivity()).getNoteNames();


        // Get the note name...
        AlertDialog.Builder inputDialog = new AlertDialog.Builder(getActivity());

        inputDialog.setTitle(R.string.enter_note_name_title);
        final AutoCompleteTextView inputTextView = new AutoCompleteTextView(getActivity());

        List<String> noteNameStrings = new NoteManager(getActivity()).getNoteNames();

        // Convert from list to array as per solution from https://stackoverflow.com/questions/9572795/convert-list-to-array-in-java
        String[] noteNameArray = new String[noteNameStrings.size()];
        noteNameStrings.toArray(noteNameArray);

        ArrayAdapter<String> autoCompleteNotesAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, noteNameArray);
        inputTextView.setAdapter(autoCompleteNotesAdapter);

        inputDialog.setView(inputTextView);

        inputDialog.setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int buttonId) {
                // Get user input string
                String textInput = inputTextView.getText().toString();

                // The database stores only uppercase note names; do a quick conversion...
                textInput = textInput.toUpperCase();

                // Perform some input validation on the entered note...
                boolean isValid = false;

                for (String noteName : validNoteNames) {
                    if (noteName.toUpperCase().equals(textInput)) {
                        // Input is valid!
                        isValid = true;

                        // Sort out any case sensitivity issues to ensure that notes appear on the composition exactly as they do in the Notes List in the database
                        textInput = noteName;
                    }
                }


                // If the note's valid, instantiate the new note and ask for a length - otherwise, error
                if (isValid) {
                    noteToAdd = new ConcreteNote(textInput);

                    // Now, ask for note length...
                    SelectNoteLengthFragment selectNoteLengthFragment = new SelectNoteLengthFragment();

                    selectNoteLengthFragment.setListener(new SelectNoteLengthFragmentCallbackListener() {
                        /**
                         * @param fragment the note length selection fragment.
                         * @param length   the length for the note, in terms of beats.
                         */
                        @Override
                        public void noteLengthSelectedFromFragment(SelectNoteLengthFragment fragment, double length) {
                            if (noteToAdd != null) {
                                noteToAdd.setLength(length);

                                // there's a note to add; add it and refresh...
                                Log.d("AddNote", ("Added note " + noteToAdd));

                                // Tell the listener...
                                if (addNoteFragmentListener != null) {
                                    addNoteFragmentListener.addNoteFragmentAddedNote(AddNoteFragment.this, noteToAdd);
                                }

                                refreshNotesList();

                                // Don't allow the same note to be added multiple times...
                                noteToAdd = null;
                            }


                        }
                    });

                    selectNoteLengthFragment.show(getActivity().getFragmentManager(), getString(R.string.select_note_length));


                } else {
                    // Display error message...
                    new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.invalid_note_error_title)
                            .setMessage(R.string.invalid_note_error_message)
                            .setPositiveButton(R.string.okay, null)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();

                }


            }
        });

        inputDialog.setNegativeButton(R.string.cancel, null);

        inputDialog.create().show();

    }

    /**
     * Ensures that the correct layout is used for this fragment.
     *
     * @param inflater           the LayoutInflater for this fragment.
     * @param container          the View containing this fragment.
     * @param savedInstanceState saved state about this fragment.
     * @return a View containing this fragment, from the XML representation.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_add_note, container, false);
    }

    /**
     * This method is called when an item in the list view of notes is tapped.
     *
     * @param parent   the ListView containing the notes.
     * @param view     the cell containing the selected note.
     * @param position the position of the selected note in the list.
     * @param id       the id of the cell containing the selected note in the list.
     */
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // If it's the Add New Note button, add new note
        if (notesForCurrentBeat.size() < 5) {
            if (position == notesForCurrentBeat.size() || (notesForCurrentBeat.size() == 0 && position == 0)) {
                // Add new note
                addNewNote();
            }
        }

    }

    /**
     * This method is called when an item in the notes list is long pressed. If it's a valid note,
     * the user is then asked if they'd like to delete it from the composition.
     *
     * @param parent   the ListView containing the notes.
     * @param view     the cell containing the selected note.
     * @param position the position of the selected note in the list.
     * @param id       the ID of the cell containing the selected note in the list.
     * @return a boolean indicating whether the currently selected note can be long pressed
     * and marked for deletion, or not.
     */
    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        // If it's the add note button, return false - otherwise - delete the note.
        if (notesForCurrentBeat.size() < 5) {
            if (position == notesForCurrentBeat.size() || (notesForCurrentBeat.size() == 0 && position == 0)) {
                // Add new note button, just return false
                return false;
            }
        }

        final Note selectedNote = notesForCurrentBeat.get(position);

        AlertDialog.Builder lengthInputDialog = new AlertDialog.Builder(getActivity());
        lengthInputDialog.setTitle(getString(R.string.note_delete_confirmation_message) + selectedNote.getPitch() + getString(R.string.question_mark));

        lengthInputDialog.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int buttonId) {
                // Tell the listener...
                if (addNoteFragmentListener != null) {
                    addNoteFragmentListener.addNoteFragmentDeletedNote(AddNoteFragment.this, selectedNote);
                }

                refreshNotesList();

            }
        });

        lengthInputDialog.setNegativeButton(R.string.cancel, null);

        lengthInputDialog.show();


        return true;
    }

    /**
     * This method is called when the user has selected an Intonation value from the intonation
     * selection fragment list, and the list has dismissed itself from the screen.
     *
     * @param fragment      the intonation selection fragment.
     * @param newIntonation the Intonation that has been selected in the fragment.
     */
    @Override
    public void intonationSelectedFromFragment(SelectIntonationFragment fragment, Intonation newIntonation) {
        Log.d("intonation selected", "intonation selected: " + newIntonation);
        currentIntonation = newIntonation;
        refreshIntonation();

        if (addNoteFragmentListener != null) {
            addNoteFragmentListener.addNoteFragmentIntonationSelected(this, currentIntonation);
        }

    }


    /**
     * Returns the current AddNoteFragmentListener object for this fragment's instance.
     *
     * @return the current AddNoteFragmentListener object for this fragment's instance.
     */
    public AddNoteFragmentListener getAddNoteFragmentListener() {
        return addNoteFragmentListener;
    }

    /**
     * Sets the AddNoteFragmentListener for this fragment, so callbacks can be sent when a note is
     * added/removed, or an intonation is selected.
     *
     * @param addNoteFragmentListener the new AddNoteFragmentListener instance for this fragment.
     */
    public void setAddNoteFragmentListener(AddNoteFragmentListener addNoteFragmentListener) {
        this.addNoteFragmentListener = addNoteFragmentListener;
    }

    /**
     * Returns the current intonation for the beat being edited
     *
     * @return the current intonation for the beat being edited in this fragment
     */
    private Intonation getCurrentIntonation() {
        return currentIntonation;
    }

    /**
     * Sets the intonation for the beat being edited in this Fragment.
     *
     * @param currentIntonation the new intonation for this fragment instance.
     */
    public void setCurrentIntonation(Intonation currentIntonation) {
        this.currentIntonation = currentIntonation;
    }

    /**
     * Returns the notes for the current beat.
     *
     * @return a List of Note objects for the current Beat.
     */
    private List<Note> getNotesForCurrentBeat() {
        return notesForCurrentBeat;
    }

    /**
     * Sets the notes for the current beat being edited.
     *
     * @param notesForCurrentBeat a List of Note objects for the current Beat being edited.
     */
    public void setNotesForCurrentBeat(List<Note> notesForCurrentBeat) {
        this.notesForCurrentBeat = notesForCurrentBeat;
    }

}
