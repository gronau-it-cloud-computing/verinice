/*******************************************************************************
 * Copyright (c) 2009 Anne Hanekop <ah[at]sernet[dot]de>
 * This program is free software: you can redistribute it and/or 
 * modify it under the terms of the GNU Lesser General Public License 
 * as published by the Free Software Foundation, either version 3 
 * of the License, or (at your option) any later version.
 *     This program is distributed in the hope that it will be useful,    
 * but WITHOUT ANY WARRANTY; without even the implied warranty 
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * See the GNU Lesser General Public License for more details.
 *     You should have received a copy of the GNU Lesser General Public 
 * License along with this program. 
 * If not, see <http://www.gnu.org/licenses/>.
 * 
 * Contributors:
 *     Anne Hanekop <ah[at]sernet[dot]de> 	- initial API and implementation
 *     ak[at]sernet[dot]de					- various fixes, adapted to command layer
 ******************************************************************************/
package sernet.gs.ui.rcp.main.bsi.risikoanalyse.wizard;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import sernet.gs.model.Gefaehrdung;
import sernet.gs.model.IGSModel;
import sernet.gs.ui.rcp.main.ExceptionUtil;
import sernet.gs.ui.rcp.main.bsi.risikoanalyse.model.OwnGefaehrdungHome;
import sernet.gs.ui.rcp.main.bsi.views.BSIKatalogInvisibleRoot;
import sernet.gs.ui.rcp.main.service.ServiceFactory;
import sernet.gs.ui.rcp.main.service.taskcommands.riskanalysis.LoadAssociatedGefaehrdungen;
import sernet.gs.ui.rcp.main.service.taskcommands.riskanalysis.UpdateRiskAnalysis;
import sernet.verinice.interfaces.CommandException;
import sernet.verinice.model.bsi.risikoanalyse.GefaehrdungsUmsetzung;
import sernet.verinice.model.bsi.risikoanalyse.GefaehrdungsUtil;
import sernet.verinice.model.bsi.risikoanalyse.OwnGefaehrdung;

/**
 * WizardPage which lists all Gefaehrdungen from BSI IT-Grundschutz-Kataloge and
 * additionally all self-defined OwnGefaehrdungen in a CheckboxTableViewer.
 * 
 * @author ahanekop[at]sernet[dot]de
 */
@SuppressWarnings("restriction")
public class ChooseGefaehrdungPage extends RiskAnalysisWizardPage<CheckboxTableViewer> {

    private TableViewerColumn checkboxColumn;
    private TableViewerColumn imageColumn;
    private TableViewerColumn numberColumn;
    private TableViewerColumn nameColumn;
    private TableViewerColumn categoryColumn;

    /**
     * Constructor sets title an description of WizardPage.
     */
    protected ChooseGefaehrdungPage() {
        super(Messages.ChooseGefaehrdungPage_0, Messages.ChooseGefaehrdungPage_1, Messages.ChooseGefaehrdungPage_2);
    }

    @Override
    protected void setColumns() {
        checkboxColumn = new TableViewerColumn(viewer, SWT.LEFT);
        checkboxColumn.getColumn().setText(""); //$NON-NLS-1$

        imageColumn = new TableViewerColumn(viewer, SWT.LEFT);
        imageColumn.getColumn().setText(""); //$NON-NLS-1$

        numberColumn = new TableViewerColumn(viewer, SWT.LEFT);
        numberColumn.getColumn().setText(Messages.ChooseGefaehrdungPage_5);

        nameColumn = new TableViewerColumn(viewer, SWT.LEFT);
        nameColumn.getColumn().setText(Messages.ChooseGefaehrdungPage_6);

        categoryColumn = new TableViewerColumn(viewer, SWT.LEFT);
        categoryColumn.getColumn().setText(Messages.ChooseGefaehrdungPage_7);
    }
    
    /**
     * Sets the control to the given visibility state.
     * 
     * @param visible
     *            boolean indicating if content should be visible
     */
    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            initContents();
        }
    }

    @Override
    protected void addSpecificListenersForPage() {
        /* Listener adds/removes Filter gefaehrdungFilter */
        buttonGefaehrdungen.addSelectionListener(new SelectionAdapter() {

            /**
             * Adds/removes Filter depending on event.
             * 
             * @param event
             *            event containing information about the selection
             */
            @Override
            public void widgetSelected(SelectionEvent event) {
                Button button = (Button) event.widget;
                if (button.getSelection()) {
                    viewer.addFilter(gefaehrdungFilter);
                    viewer.refresh();
                    checkAllSelectedGefaehrdungen();
                } else {
                    viewer.removeFilter(gefaehrdungFilter);
                    refresh();
                    assignBausteinGefaehrdungen();
                    checkAllSelectedGefaehrdungen();
                }
            }
        });

        /* Listener adds/removes Filter ownGefaehrdungFilter */
        buttonOwnGefaehrdungen.addSelectionListener(new SelectionAdapter() {

            /**
             * Adds/removes Filter depending on event.
             * 
             * @param event
             *            event containing information about the selection
             */
            @Override
            public void widgetSelected(SelectionEvent event) {
                Button button = (Button) event.widget;
                if (button.getSelection()) {
                    viewer.addFilter(ownGefaehrdungFilter);
                    viewer.refresh();
                    checkAllSelectedGefaehrdungen();
                } else {
                    viewer.removeFilter(ownGefaehrdungFilter);
                    viewer.refresh();
                    assignBausteinGefaehrdungen();
                    checkAllSelectedGefaehrdungen();
                }
            }
        });

        /*
         * listener adds/removes Gefaehrdungen to Array of selected
         * Gefaehrdungen
         */
        viewer.addCheckStateListener(new ICheckStateListener() {
            @Override
            public void checkStateChanged(CheckStateChangedEvent event) {
                Gefaehrdung currentGefaehrdung = (Gefaehrdung) event.getElement();
                associateGefaehrdung(currentGefaehrdung, event.getChecked());
            }

        });

        /* listener opens edit Dialog for the selected Gefaehrdung */
        viewer.addDoubleClickListener(new IDoubleClickListener() {

            /**
             * Notifies of a double click.
             * 
             * @param event
             *            event object describing the double-click
             */
            @Override
            public void doubleClick(DoubleClickEvent event) {
                /* retrieve selected Gefaehrdung and open edit dialog with it */
                IStructuredSelection selection = (IStructuredSelection) event.getSelection();
                IGSModel selectedGefaehrdung = (IGSModel) selection.getFirstElement();
                if (selectedGefaehrdung instanceof OwnGefaehrdung) {
                    OwnGefaehrdung selectedOwnGefaehrdung = (OwnGefaehrdung) selectedGefaehrdung;
                    final EditGefaehrdungDialog dialog = new EditGefaehrdungDialog(rootContainer.getShell(), selectedOwnGefaehrdung);
                    dialog.open();
                    refresh();
                }
            }
        });


        /* Listener adds/removes Filter searchFilter */
        textSearch.addModifyListener(new ModifyListener() {

            /**
             * Adds/removes Filter when Text is modified depending on event.
             * 
             * @param event
             *            event containing information about the selection
             */
            @Override
            public void modifyText(ModifyEvent event) {
                Text text = (Text) event.widget;
                if (text.getText().length() > 0) {

                    ViewerFilter[] filters = viewer.getFilters();
                    RiskAnalysisWizardPageSearchFilter thisFilter = null;
                    boolean contains = false;

                    for (ViewerFilter item : filters) {
                        if (item instanceof RiskAnalysisWizardPageSearchFilter) {
                            contains = true;
                            thisFilter = (RiskAnalysisWizardPageSearchFilter) item;
                        }
                    }
                    if (contains) {
                        /* filter is already active - update filter */
                        thisFilter.setPattern(text.getText());
                        viewer.refresh();
                        checkAllSelectedGefaehrdungen();

                    } else {
                        /* filter is not active - add */
                        searchFilter.setPattern(text.getText());
                        viewer.addFilter(searchFilter);
                        viewer.refresh();
                        checkAllSelectedGefaehrdungen();
                    }
                } else {
                    viewer.removeFilter(searchFilter);
                    viewer.refresh();
                    assignBausteinGefaehrdungen();
                    checkAllSelectedGefaehrdungen();
                }
            }
        });

        /* Listener opens MessageDialog and deletes selected Gefaehrdung */
        buttonDelete.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
                Gefaehrdung selectedGefaehrdung = (Gefaehrdung) selection.getFirstElement();
                if (selectedGefaehrdung instanceof OwnGefaehrdung) {
                    /* ask user to confirm */
                    boolean confirmed = MessageDialog.openQuestion(rootContainer.getShell(), Messages.ChooseGefaehrdungPage_14, NLS.bind(Messages.ChooseGefaehrdungPage_15, selectedGefaehrdung.getTitel()));
                    if (confirmed) {
                        deleteOwnGefaehrdung(selectedGefaehrdung);
                        viewer.refresh();
                        assignBausteinGefaehrdungen();
                    }
                }
            }
        });

        /* Listener opens Dialog for creation of new OwnGefaehrdung */
        buttonNew.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                List<OwnGefaehrdung> arrListOwnGefaehrdungen = ((RiskAnalysisWizard) getWizard()).getAllOwnGefaehrdungen();
                final NewGefaehrdungDialog dialog = new NewGefaehrdungDialog(rootContainer.getShell(), arrListOwnGefaehrdungen);
                dialog.open();
                ((RiskAnalysisWizard) getWizard()).addOwnGefaehrdungen();
                viewer.refresh();
                assignBausteinGefaehrdungen();
            }
        });
        /* Listener opens Dialog for editing the selected Gefaehrdung */
        buttonEdit.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
                IGSModel selectedGefaehrdung = (IGSModel) selection.getFirstElement();
                if (selectedGefaehrdung instanceof OwnGefaehrdung) {
                    OwnGefaehrdung ownGefSelected = (OwnGefaehrdung) selectedGefaehrdung;
                    final EditGefaehrdungDialog dialog = new EditGefaehrdungDialog(rootContainer.getShell(), ownGefSelected);
                    dialog.open();
                    refresh();
                }
            }
        });

    }

    protected void associateGefaehrdung(Gefaehrdung currentGefaehrdung, boolean select) {

        if (select) {
            getRiskAnalysisWizard().addAssociatedGefaehrdung(currentGefaehrdung);
        } else {
            try {
                getRiskAnalysisWizard().removeAssociatedGefaehrdung(currentGefaehrdung);
            } catch (Exception e) {
                ExceptionUtil.log(e, NLS.bind(Messages.ChooseGefaehrdungPage_18, currentGefaehrdung.getTitel()));
            }
        }

        checkPageComplete();

    }

    /**
     * Fills the CheckboxTableViewer with all Gefaehrdungen available. Is
     * processed each time the WizardPage is set visible.
     */
    private void initContents() {

        ArrayList<Gefaehrdung> arrListAllGefaehrdungen = (ArrayList<Gefaehrdung>) getRiskAnalysisWizard().getAllGefaehrdungen();

        /* map a domain model object into multiple images and text labels */
        viewer.setLabelProvider(new CheckboxTableViewerLabelProvider());

        /* map domain model into array */
        viewer.setContentProvider(new ArrayContentProvider());
        /* associate domain model with viewer */
        viewer.setInput(arrListAllGefaehrdungen);
        viewer.setSorter(new GefaehrdungenSorter());
        viewer.addSelectionChangedListener(new ISelectionChangedListener() {

            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                if (event.getSelection() instanceof IStructuredSelection) {
                    if (((IStructuredSelection) event.getSelection()).getFirstElement() instanceof OwnGefaehrdung) {
                        buttonDelete.setEnabled(true);
                        buttonEdit.setEnabled(true);
                    } else {
                        buttonDelete.setEnabled(false);
                        buttonEdit.setEnabled(false);
                    }
                }
                //
            }
        });
        assignBausteinGefaehrdungen();
        checkAllSelectedGefaehrdungen();
        packAllColumns();

        ((RiskAnalysisWizard) getWizard()).setCanFinish(false);
        checkPageComplete();
    }

    private void checkAllSelectedGefaehrdungen() {
        List<Gefaehrdung> toCheck = new ArrayList<Gefaehrdung>();
        for (GefaehrdungsUmsetzung associatedGefaehrdung : getRiskAnalysisWizard().getAssociatedGefaehrdungen()) {
            if (associatedGefaehrdung != null) {
                for (Gefaehrdung gefaehrdung : getRiskAnalysisWizard().getAllGefaehrdungen()) {
                    if (gefaehrdung != null && gefaehrdung.getId() != null && gefaehrdung.getId().equals(associatedGefaehrdung.getId())) {
                        toCheck.add(gefaehrdung);
                    }
                }
            }
        }

        Gefaehrdung[] checkarray = toCheck.toArray(new Gefaehrdung[toCheck.size()]);
        viewer.setCheckedElements(checkarray);
    }

    /**
     * Marks all checkboxes of Gefaehrdungen associated to the selected
     * Baustein.
     */
    private void assignBausteinGefaehrdungen() {
        try {
            LoadAssociatedGefaehrdungen command = new LoadAssociatedGefaehrdungen(getRiskAnalysisWizard().getCnaElement(), BSIKatalogInvisibleRoot.getInstance().getLanguage());
            command = ServiceFactory.lookupCommandService().executeCommand(command);
            List<GefaehrdungsUmsetzung> list = command.getAssociatedGefaehrdungen();

            for (GefaehrdungsUmsetzung selectedGefaehrdung : list) {
                if (selectedGefaehrdung != null) {
                    for (Gefaehrdung gefaehrdung : getRiskAnalysisWizard().getAllGefaehrdungen()) {
                        if (gefaehrdung != null && gefaehrdung.getId() != null && gefaehrdung.getId().equals(selectedGefaehrdung.getId())) {
                            associateGefaehrdung(gefaehrdung, true);
                        }
                    }
                }
            }
            UpdateRiskAnalysis updateCommand = new UpdateRiskAnalysis(getRiskAnalysisWizard().getFinishedRiskAnalysisLists());
            updateCommand = ServiceFactory.lookupCommandService().executeCommand(updateCommand);
        } catch (CommandException e) {
            ExceptionUtil.log(e, ""); //$NON-NLS-1$
        }
    }

    /**
     * Adjusts all columns of the CheckboxTableViewer.
     */
    private void packAllColumns() {
        checkboxColumn.getColumn().pack();
        imageColumn.getColumn().pack();
        numberColumn.getColumn().pack();
        nameColumn.getColumn().pack();
        nameColumn.getColumn().setWidth(Math.min(nameColumn.getColumn().getWidth(), WIDTH_COL_NAME));
        categoryColumn.getColumn().pack();
    }

    /**
     * Activates the next button, if the List of selected Gefaehrdungen is not
     * empty.
     */
    private void checkPageComplete() {
        if (((RiskAnalysisWizard) getWizard()).getAssociatedGefaehrdungen().isEmpty()) {
            setPageComplete(false);
        } else {
            setPageComplete(true);
        }
    }





    /**
     * Filter to extract all OwnGefaehrdungen in CheckboxTableViewer.
     * 
     * @author ahanekop[at]sernet[dot]de
     */
    static class OwnGefaehrdungenFilter extends ViewerFilter {

        /**
         * Returns true, if the given element is an OwnGefaehrdung.
         * 
         * @param viewer
         *            the Viewer to operate on
         * @param parentElement
         *            not used
         * @param element
         *            given element
         * @return true if element passes test, false else
         */
        @Override
        public boolean select(Viewer viewer, Object parentElement, Object element) {
            if (element instanceof OwnGefaehrdung) {
                return true;
            }
            return false;
        }
    }

    /**
     * Filter to extract all Gefaehrdungen in CheckboxTableViewer.
     * 
     * @author ahanekop[at]sernet[dot]de
     */
    static class GefaehrdungenFilter extends ViewerFilter {

        /**
         * Returns true, if the given element is a Gefaehrdung.
         * 
         * @param viewer
         *            the Viewer to operate on
         * @param parentElement
         *            not used
         * @param element
         *            given element
         * @return true if element passes test, false else
         */
        @Override
        public boolean select(Viewer viewer, Object parentElement, Object element) {
            if (!(element instanceof OwnGefaehrdung)) {
                return true;
            }
            return false;
        }
    }


    /**
     * Deletes a OwnGefaehrdung.
     * 
     * @param delGefaehrdung
     *            the (Own)Gefaehrdung to delete
     */
    private void deleteOwnGefaehrdung(Gefaehrdung delGefaehrdung) {
        ArrayList<Gefaehrdung> arrListAllGefaehrdungen = (ArrayList<Gefaehrdung>) ((RiskAnalysisWizard) getWizard()).getAllGefaehrdungen();
        List<GefaehrdungsUmsetzung> arrListAssociatedGefaehrdungen = ((RiskAnalysisWizard) getWizard()).getAssociatedGefaehrdungen();
        List<OwnGefaehrdung> arrListOwnGefaehrdungen = ((RiskAnalysisWizard) getWizard()).getAllOwnGefaehrdungen();

        try {
            if (arrListOwnGefaehrdungen.contains(delGefaehrdung)) {
                /* delete OwnGefaehrdung from Database */
                OwnGefaehrdungHome.getInstance().remove((OwnGefaehrdung) delGefaehrdung);

                /* delete OwnGefaehrdung from List of OwnGefaehrdungen */
                arrListOwnGefaehrdungen.remove(delGefaehrdung);

                /* delete OwnGefaehrdung from List of selected Gefaehrdungen */
                GefaehrdungsUtil.removeBySameId(arrListAssociatedGefaehrdungen, delGefaehrdung);

                /* delete OwnGefaehrdung from list of all Gefaehrdungen */
                if (arrListAllGefaehrdungen.contains(delGefaehrdung)) {
                    arrListAllGefaehrdungen.remove(delGefaehrdung);
                }
            }
        } catch (Exception e) {
            ExceptionUtil.log(e, Messages.ChooseGefaehrdungPage_20);
        }
    }


    @Override
    protected CheckboxTableViewer initializeViewer(Composite parent) {
        return CheckboxTableViewer.newCheckList(parent, SWT.BORDER | SWT.FULL_SELECTION);
    }

}
