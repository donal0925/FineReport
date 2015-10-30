package com.fr.design.condition;

import com.fr.base.BaseUtils;
import com.fr.base.Formula;
import com.fr.data.DataConstants;
import com.fr.data.condition.*;
import com.fr.design.beans.BasicBeanPane;
import com.fr.design.dialog.DialogActionAdapter;
import com.fr.design.formula.FormulaFactory;
import com.fr.design.formula.UIFormula;
import com.fr.design.formula.VariableResolver;
import com.fr.design.gui.ibutton.UIButton;
import com.fr.design.gui.ibutton.UIRadioButton;
import com.fr.design.gui.ilable.UILabel;
import com.fr.design.gui.itextarea.UITextArea;
import com.fr.design.gui.itree.refreshabletree.ExpandMutableTreeNode;
import com.fr.design.layout.FRGUIPaneFactory;
import com.fr.design.scrollruler.ModLineBorder;
import com.fr.design.utils.gui.GUICoreUtils;
import com.fr.general.ComparatorUtils;
import com.fr.general.Inter;
import com.fr.general.data.Condition;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * peter: LiteCondition Pane.
 */
public abstract class LiteConditionPane<T extends Condition> extends BasicBeanPane<Condition> {
    private static int MOVE_UP = 0;
    private static int MOVE_DOWN = 1;

    private static final long serialVersionUID = 1L;
    // peter:�����������ڵ�����ʽ�༭����ʱ��,��Ҫ��.
    private UIRadioButton commonRadioButton = new UIRadioButton(Inter.getLocText("Common"));
    private UIRadioButton formulaRadioButton = new UIRadioButton(Inter.getLocText("Formula"));
    private JPanel conditionCardPane;
    protected BasicBeanPane<T> defaultConditionPane;
    // card2
    private UITextArea formulaTextArea;
    private UIButton modifyButton;
    private UIButton addButton;
    private UIRadioButton andRadioButton = new UIRadioButton(Inter.getLocText("ConditionB-AND") + "  ");
    private UIRadioButton orRadioButton = new UIRadioButton(Inter.getLocText("ConditionB-OR"));
    protected JTree conditionsTree;// Conditions
    private UIButton removeButton;
    private UIButton moveUpButton;
    private UIButton moveDownButton;
    private UIButton bracketButton;
    private UIButton unBracketButton;

    private ActionListener actionListener1 = new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent evt) {
            Formula formula;

            String text = formulaTextArea.getText();
            if (text == null || text.length() <= 0) {
                formula = new Formula("");
            } else {
                formula = new Formula(text);
            }

            final UIFormula formulaPane = FormulaFactory.createFormulaPane();
            formulaPane.populate(formula, variableResolver4FormulaPane());
            formulaPane.showLargeWindow(SwingUtilities.getWindowAncestor(LiteConditionPane.this), new DialogActionAdapter() {

                @Override
                public void doOk() {
                    Formula formula = formulaPane.update();
                    if (formula.getContent().length() <= 1) {// ���û�����κ��ַ������ǿհ��ı�
                        formulaTextArea.setText("");
                    } else {
                        formulaTextArea.setText(formula.getContent().substring(1));
                    }
                }
            }).setVisible(true);
        }
    };


    private ActionListener actionListener2 = new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent evt) {
            // peter:�Ȼ�õ�ǰ��LiteCondition.

            Condition liteCondition = null;
            if (commonRadioButton.isSelected()) {
                liteCondition = defaultConditionPane.updateBean();
            } else {
                liteCondition = new FormulaCondition(formulaTextArea.getText());
            }

            JoinCondition newJoinCondition = new JoinCondition(andRadioButton.isSelected() ? DataConstants.AND : DataConstants.OR, liteCondition);
            ExpandMutableTreeNode parentTreeNode = getParentTreeNode();
            boolean result = isExistedInParentTreeNode(parentTreeNode, newJoinCondition);
            if (result) {
                JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(LiteConditionPane.this),
                        Inter.getLocText("BindColumn-This_Condition_has_been_existed"));
                return;
            }

            ExpandMutableTreeNode newJionConditionTreeNode = new ExpandMutableTreeNode(newJoinCondition);
            parentTreeNode.add(newJionConditionTreeNode);
            DefaultTreeModel defaultTreeModel = (DefaultTreeModel) conditionsTree.getModel();
            defaultTreeModel.reload(parentTreeNode);
            parentTreeNode.expandCurrentTreeNode(conditionsTree);
            conditionsTree.setSelectionPath(GUICoreUtils.getTreePath(newJionConditionTreeNode));

            // peter:����Ҫ���Enabled.
            checkButtonEnabledForList();
        }
    };


    private MouseAdapter mouseAdapter = new MouseAdapter() {

        @Override
        public void mouseEntered(MouseEvent evt) {
            TreePath selectedTreePath = conditionsTree.getSelectionPath();
            // peter:��ǰ�Ľڵ�
            if (selectedTreePath != null) {
                ExpandMutableTreeNode selectedTreeNode = (ExpandMutableTreeNode) selectedTreePath.getLastPathComponent();
                JoinCondition oldJoinCondition = (JoinCondition) selectedTreeNode.getUserObject();
                oldJoinCondition.setJoin(andRadioButton.isSelected() ? DataConstants.AND : DataConstants.OR);

                Condition oldLiteCondition = oldJoinCondition.getCondition();
                // peter:�����ǰѡ�е���ListCondition,ֻҪ�ı�JoinΪAND����OR,ֱ�ӷ���.
                if (oldLiteCondition instanceof ListCondition) {
                    GUICoreUtils.setEnabled(conditionCardPane, false);
                }
            }
        }

        @Override
        public void mouseExited(MouseEvent evt) {
        	GUICoreUtils.setEnabled(conditionCardPane, conditionCardPane.isEnabled());
        }
    };

    private TreeSelectionListener treeSelectionListener = new TreeSelectionListener() {

        @Override
        public void valueChanged(TreeSelectionEvent evt) {
            checkButtonEnabledForList();

            TreePath selectedTreePath = conditionsTree.getSelectionPath();
            if (selectedTreePath == null) {
                return;
            }

            ExpandMutableTreeNode selectedTreeNode = (ExpandMutableTreeNode) selectedTreePath.getLastPathComponent();
            Object userObject = selectedTreeNode.getUserObject();
            if (userObject instanceof JoinCondition) {
                JoinCondition joinCondition = (JoinCondition) userObject;

                // peter:��Ūjoin.
                int join = joinCondition.getJoin();
                if (join == DataConstants.AND) {
                    andRadioButton.setSelected(true);
                } else {
                    orRadioButton.setSelected(true);
                }

                // peter:��ǰ��liteCondtion.
                Condition liteCondition = joinCondition.getCondition();
                // elake:����Condition��Ӧ�������к͸���.
                if (liteCondition instanceof CommonCondition || liteCondition instanceof ObjectCondition) {
                    Condition commonCondition = liteCondition;
                    commonRadioButton.setSelected(true);
                    applyCardsPane();

                    defaultConditionPane.populateBean((T) commonCondition);

                } else if (liteCondition instanceof FormulaCondition) {
                    FormulaCondition formulaCondition = (FormulaCondition) liteCondition;

                    formulaRadioButton.setSelected(true);
                    applyCardsPane();

                    formulaTextArea.setText(formulaCondition.getFormula());
                }
            }
        }
    };


    private ActionListener actionListener3 = new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent evt) {
            TreePath selectedTreePath = conditionsTree.getSelectionPath();
            if (selectedTreePath == null) {
                return;
            }

            int returnVal = JOptionPane.showConfirmDialog(SwingUtilities.getWindowAncestor(LiteConditionPane.this),
                    Inter.getLocText("Utils-Are_you_sure_to_remove_the_selected_item") + "?", Inter.getLocText("Remove"),
                    JOptionPane.OK_CANCEL_OPTION);
            if (returnVal == JOptionPane.OK_OPTION) {
                DefaultTreeModel treeModel = (DefaultTreeModel) conditionsTree.getModel();

                TreePath[] selectedTreePaths = conditionsTree.getSelectionPaths();
                for (int i = selectedTreePaths.length - 1; i >= 0; i--) {
                    ExpandMutableTreeNode selectedTreeNode = (ExpandMutableTreeNode) selectedTreePaths[i].getLastPathComponent();
                    ExpandMutableTreeNode parentTreeNode = (ExpandMutableTreeNode) selectedTreeNode.getParent();

                    // peter:��Ҫѡ��ڵ�.
                    ExpandMutableTreeNode nextSelectTreeNode;
                    if (parentTreeNode.getChildAfter(selectedTreeNode) != null) {
                        nextSelectTreeNode = (ExpandMutableTreeNode) parentTreeNode.getChildAfter(selectedTreeNode);
                    } else if (parentTreeNode.getChildBefore(selectedTreeNode) != null) {
                        nextSelectTreeNode = (ExpandMutableTreeNode) parentTreeNode.getChildBefore(selectedTreeNode);
                    } else {
                        nextSelectTreeNode = parentTreeNode;
                    }

                    parentTreeNode.remove(selectedTreeNode);

                    if (!ComparatorUtils.equals(nextSelectTreeNode, treeModel.getRoot())) {
                        conditionsTree.setSelectionPath(GUICoreUtils.getTreePath(nextSelectTreeNode));
                    }
                    treeModel.reload(parentTreeNode);
                    parentTreeNode.expandCurrentTreeNode(conditionsTree);
                    if (!ComparatorUtils.equals(nextSelectTreeNode, treeModel.getRoot())) {
                        conditionsTree.setSelectionPath(GUICoreUtils.getTreePath(nextSelectTreeNode));
                    }
                }

                // peter:���Button Enabled.
                checkButtonEnabledForList();
            }
        }
    };


    private ActionListener actionListener4 = new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent evt) {
            TreePath selectedTreePath = conditionsTree.getSelectionPath();
            if (selectedTreePath == null) {
                return;
            }
            ExpandMutableTreeNode selectedTreeNode = (ExpandMutableTreeNode) selectedTreePath.getLastPathComponent();
            ExpandMutableTreeNode parentTreeNode = (ExpandMutableTreeNode) selectedTreeNode.getParent();
            if (parentTreeNode.getChildBefore(selectedTreeNode) != null) {
                swapNodesOfConditionTree(parentTreeNode, (ExpandMutableTreeNode) parentTreeNode.getChildBefore(selectedTreeNode),
                        selectedTreeNode, MOVE_UP);
            }
        }
    };


    private ActionListener actionListener5 = new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent evt) {
            TreePath selectedTreePath = conditionsTree.getSelectionPath();
            if (selectedTreePath == null) {
                return;
            }
            ExpandMutableTreeNode selectedTreeNode = (ExpandMutableTreeNode) selectedTreePath.getLastPathComponent();
            ExpandMutableTreeNode parentTreeNode = (ExpandMutableTreeNode) selectedTreeNode.getParent();
            if (parentTreeNode.getChildAfter(selectedTreeNode) != null) {
                swapNodesOfConditionTree(parentTreeNode, selectedTreeNode, (ExpandMutableTreeNode) parentTreeNode.getChildAfter(selectedTreeNode), MOVE_DOWN);
            }
        }
    };


    private ActionListener actionListener6 = new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent evt) {
            TreePath[] selectedTreePaths = conditionsTree.getSelectionPaths();
            // peter:��ǰ�Ľڵ�
            if (selectedTreePaths == null || selectedTreePaths.length <= 1) {
                return;
            }

            // peter: �ҵ����׽ڵ�,����ɾ�����еĽڵ�.
            TreePath topTreePath = GUICoreUtils.getTopTreePath(conditionsTree, selectedTreePaths);
            ExpandMutableTreeNode leadTreeNode = (ExpandMutableTreeNode) topTreePath.getLastPathComponent();
            ExpandMutableTreeNode parentTreeNode = (ExpandMutableTreeNode) leadTreeNode.getParent();
            int topIndex = parentTreeNode.getIndex(leadTreeNode);

            JoinCondition firstJionCondition = (JoinCondition) leadTreeNode.getUserObject();
            for (int i = 0; i < selectedTreePaths.length; i++) {
                ExpandMutableTreeNode tmpTreeNode = (ExpandMutableTreeNode) selectedTreePaths[i].getLastPathComponent();
                parentTreeNode.remove(tmpTreeNode);
            }

            // peter:�����µĽڵ�.
            JoinCondition newJionCondition = new JoinCondition();
            newJionCondition.setJoin(firstJionCondition.getJoin());
            newJionCondition.setCondition(new ListCondition());
            ExpandMutableTreeNode newTreeNode = new ExpandMutableTreeNode(newJionCondition);
            for (int i = 0; i < selectedTreePaths.length; i++) {
                ExpandMutableTreeNode tmpTreeNode = (ExpandMutableTreeNode) selectedTreePaths[i].getLastPathComponent();
                newTreeNode.add(tmpTreeNode);
            }

            // peter:�����µĽڵ�
            parentTreeNode.insert(newTreeNode, topIndex);

            // peter:��Ҫreload
            DefaultTreeModel defaultTreeModel = (DefaultTreeModel) conditionsTree.getModel();
            defaultTreeModel.reload(parentTreeNode);
            parentTreeNode.expandCurrentTreeNode(conditionsTree);

            // peter:ѡ��һ���ڵ�
            conditionsTree.setSelectionPath(GUICoreUtils.getTreePath(newTreeNode));
        }
    };


    ActionListener actionListener7 = new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent evt) {
            TreePath selectedTreePath = conditionsTree.getSelectionPath();
            // peter:��ǰ�Ľڵ�
            if (selectedTreePath == null) {
                return;
            }

            ExpandMutableTreeNode selectedTreeNode = (ExpandMutableTreeNode) selectedTreePath.getLastPathComponent();
            JoinCondition joinCondition = (JoinCondition) selectedTreeNode.getUserObject();
            Condition liteCondition = joinCondition.getCondition();
            if (liteCondition instanceof ListCondition) {
                ExpandMutableTreeNode parentTreeNode = (ExpandMutableTreeNode) selectedTreeNode.getParent();
                int index = parentTreeNode.getIndex(selectedTreeNode);

                // peter:���ӽڵ�
                List<TreePath> treePathList = new ArrayList<TreePath>();
                for (int i = selectedTreeNode.getChildCount() - 1; i >= 0; i--) {
                    ExpandMutableTreeNode tmpTreeNode = (ExpandMutableTreeNode) selectedTreeNode.getChildAt(i);
                    parentTreeNode.insert(tmpTreeNode, index);
                    treePathList.add(GUICoreUtils.getTreePath(tmpTreeNode));
                }

                // peter;ɾ�����List�ڵ�
                parentTreeNode.remove(selectedTreeNode);

                // peter:��Ҫreload
                DefaultTreeModel defaultTreeModel = (DefaultTreeModel) conditionsTree.getModel();
                defaultTreeModel.reload(parentTreeNode);
                parentTreeNode.expandCurrentTreeNode(conditionsTree);

                // peter:ѡ������ѡ��Ľڵ�
                TreePath[] selectedTreePaths = new TreePath[treePathList.size()];
                treePathList.toArray(selectedTreePaths);
                conditionsTree.setSelectionPaths(selectedTreePaths);
            }
        }
    };


    private ActionListener actionListener8 = new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent evt) {
            modify();
        }
    };


    // ͼ����������ʱû�й�ʽѡ��
    protected JPanel conditonTypePane;

    public LiteConditionPane() {
        this.initComponents();
    }

    protected abstract BasicBeanPane<T> createUnFormulaConditionPane();

    protected abstract VariableResolver variableResolver4FormulaPane();

    protected void initComponents() {
        this.setLayout(FRGUIPaneFactory.createBorderLayout());

        // north
        initNorth();

        //center
        JPanel centerPane = FRGUIPaneFactory.createBorderLayout_S_Pane();
        this.add(centerPane, BorderLayout.CENTER);
        centerPane.setLayout(FRGUIPaneFactory.createBorderLayout());

        // Control
        JPanel controlPane = FRGUIPaneFactory.createBorderLayout_S_Pane();
        centerPane.add(controlPane, BorderLayout.NORTH);
        // controlPane.setLayout(FRGUIPaneFactory.createBorderLayout());

        // conditionCardPane
        initConditionCardPane(controlPane);

        // addControlPane, contains or,and Radio, add,modify Button
        initControlPane(controlPane);

        // Preview
        JPanel previewPane = FRGUIPaneFactory.createBorderLayout_S_Pane();
        centerPane.add(previewPane, BorderLayout.CENTER);
        previewPane.setBorder(BorderFactory.createEmptyBorder(0, 2, 2, 0));


        // conTreeScrollPane.setPreferredSize(new Dimension(400, 125));
        previewPane.add(iniTreeScrollPane(), BorderLayout.CENTER);
        conditionsTree.addTreeSelectionListener(treeSelectionListener);

        JPanel buttonPane = FRGUIPaneFactory.createNColumnGridInnerContainer_S_Pane(1);
        previewPane.add(GUICoreUtils.createBorderPane(buttonPane, BorderLayout.NORTH), BorderLayout.EAST);
        initButtonPane(buttonPane);

        // peter:����Ҫ���Enabled.
        checkButtonEnabledForList();
    }


    private void initButtonPane(JPanel buttonPane) {
        removeButton = new UIButton(Inter.getLocText("Remove"));
        buttonPane.add(removeButton);
        removeButton.setIcon(BaseUtils.readIcon("com/fr/base/images/cell/control/remove.png"));
        removeButton.setEnabled(false);
        removeButton.addActionListener(actionListener3);

        moveUpButton = new UIButton(Inter.getLocText("Utils-Move_Up"));
        buttonPane.add(moveUpButton);
        moveUpButton.setIcon(BaseUtils.readIcon("com/fr/design/images/control/up.png"));
        moveUpButton.addActionListener(actionListener4);

        moveDownButton = new UIButton(Inter.getLocText("Utils-Move_Down"));
        buttonPane.add(moveDownButton);
        moveDownButton.setIcon(BaseUtils.readIcon("com/fr/design/images/control/down.png"));
        moveDownButton.addActionListener(actionListener5);

        // peter:������
        bracketButton = new UIButton(Inter.getLocText("ConditionB-Add_bracket"));
        buttonPane.add(bracketButton);
        bracketButton.setIcon(BaseUtils.readIcon("com/fr/design/images/condition/bracket.png"));
        bracketButton.addActionListener(actionListener6);

        // peter:ȥ������
        unBracketButton = new UIButton(Inter.getLocText("ConditionB-Remove_bracket"));
        buttonPane.add(unBracketButton);
        unBracketButton.setIcon(BaseUtils.readIcon("com/fr/design/images/condition/unBracket.png"));
        unBracketButton.addActionListener(actionListener7);
    }

    private JScrollPane iniTreeScrollPane() {
        conditionsTree = new JTree(new DefaultTreeModel(new ExpandMutableTreeNode(new JoinCondition(DataConstants.AND, new ListCondition()))));
        conditionsTree.setRootVisible(false);
        conditionsTree.setCellRenderer(conditionsTreeCellRenderer);
        conditionsTree.setSelectionModel(new ContinuousTreeSelectionModel());
        conditionsTree.addTreeExpansionListener(treeExpansionListener);
        conditionsTree.setShowsRootHandles(true);
        return new JScrollPane(conditionsTree);

    }

    private void initNorth() {
        conditonTypePane = FRGUIPaneFactory.createBorderLayout_S_Pane();
        this.add(conditonTypePane, BorderLayout.NORTH);
        conditonTypePane.setBorder(new ModLineBorder(ModLineBorder.BOTTOM));

        UILabel conditionTypeLabel = new UILabel(Inter.getLocText("Type") + ":");
        conditonTypePane.add(conditionTypeLabel, BorderLayout.WEST);
        conditionTypeLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));

        JPanel northPane = FRGUIPaneFactory.createNColumnGridInnerContainer_S_Pane(2);
        conditonTypePane.add(northPane, BorderLayout.CENTER);
        northPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        northPane.add(GUICoreUtils.createFlowPane(commonRadioButton, FlowLayout.CENTER));
        northPane.add(GUICoreUtils.createFlowPane(formulaRadioButton, FlowLayout.CENTER));
        commonRadioButton.addActionListener(radioActionListener);
        formulaRadioButton.addActionListener(radioActionListener);

        ButtonGroup mainBg = new ButtonGroup();
        mainBg.add(commonRadioButton);
        mainBg.add(formulaRadioButton);
        commonRadioButton.setSelected(true);
    }

    private void initConditionCardPane(JPanel controlPane) {
        conditionCardPane = FRGUIPaneFactory.createCardLayout_S_Pane();
        controlPane.add(conditionCardPane, BorderLayout.CENTER);
        conditionCardPane.setLayout(new CardLayout());
        conditionCardPane.setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 0));

        // defaultConditionPane
        conditionCardPane.add(defaultConditionPane = createUnFormulaConditionPane(), "DEFAULT");

        // formulaConditionPane
        JPanel formulaConditionPane = FRGUIPaneFactory.createBorderLayout_S_Pane();
        conditionCardPane.add(formulaConditionPane, "FORMULA");
        // formulaConditionPane.setLayout(FRGUIPaneFactory.createBorderLayout());

        // formulaPane
        JPanel formulaPane = FRGUIPaneFactory.createBorderLayout_S_Pane();
        formulaConditionPane.add(formulaPane, BorderLayout.CENTER);
        formulaPane.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 2));
        formulaPane.add(GUICoreUtils.createBorderPane(new UILabel(Inter.getLocText("Formula") + "="), BorderLayout.NORTH), BorderLayout.WEST);
        formulaTextArea = new UITextArea();
        formulaPane.add(new JScrollPane(formulaTextArea), BorderLayout.CENTER);
        UIButton editFormulaButton = new UIButton(Inter.getLocText("Define"));
        formulaPane.add(GUICoreUtils.createBorderPane(editFormulaButton, BorderLayout.NORTH), BorderLayout.EAST);
        editFormulaButton.addActionListener(actionListener1);
        applyCardsPane();
    }


    private void initControlPane(JPanel controlPane) {
        JPanel addControlPane = FRGUIPaneFactory.createRightFlowInnerContainer_S_Pane();
        controlPane.add(addControlPane, BorderLayout.SOUTH);
        addControlPane.setBorder(new ModLineBorder(ModLineBorder.TOP));

        ButtonGroup bg = new ButtonGroup();
        bg.add(andRadioButton);
        bg.add(orRadioButton);

        andRadioButton.setSelected(true);

        JPanel radioPane = FRGUIPaneFactory.createNColumnGridInnerContainer_S_Pane(3);
        addControlPane.add(radioPane);
        radioPane.add(andRadioButton);
        addControlPane.add(Box.createHorizontalStrut(4));
        radioPane.add(orRadioButton);

        addControlPane.add(Box.createHorizontalStrut(12));

        addButton = new UIButton(Inter.getLocText("Add"), BaseUtils.readIcon("com/fr/base/images/cell/control/add.png"));
        addButton.setMnemonic('A');
        addControlPane.add(addButton);
        addButton.addActionListener(actionListener2);

        addControlPane.add(Box.createHorizontalStrut(4));

        modifyButton = new UIButton(Inter.getLocText("Modify"), BaseUtils.readIcon("com/fr/base/images/cell/control/rename.png"));
        modifyButton.setMnemonic('M');
        addControlPane.add(modifyButton);
        modifyButton.addActionListener(actionListener8);

        // peter:���������޸İ�ť��ʱ��,�����ListConditon���ݱ༭���򲻿ɱ༭
        modifyButton.addMouseListener(mouseAdapter);
    }


    @Override
    protected String title4PopupWindow() {
        return Inter.getLocText("FR-Designer-Submit_Condition");
    }

    // samuel:�Ƴ������������
    protected void modify() {
        TreePath selectedTreePath = conditionsTree.getSelectionPath();
        // peter:��ǰ�Ľڵ�
        if (selectedTreePath != null) {
            ExpandMutableTreeNode selectedTreeNode = (ExpandMutableTreeNode) selectedTreePath.getLastPathComponent();
            JoinCondition oldJoinCondition = (JoinCondition) selectedTreeNode.getUserObject();
            oldJoinCondition.setJoin(andRadioButton.isSelected() ? DataConstants.AND : DataConstants.OR);

            Condition oldLiteCondition = oldJoinCondition.getCondition();
            // peter:�����ǰѡ�е���ListCondition,ֻҪ�ı�JoinΪAND����OR,ֱ�ӷ���.
            if (oldLiteCondition != null && !(oldLiteCondition instanceof ListCondition)) {
                // peter:�Ȼ�õ�ǰ��LiteCondition.
                Condition liteCondition;
                if (commonRadioButton.isSelected()) {
                    liteCondition = defaultConditionPane.updateBean();
                } else {
                    liteCondition = new FormulaCondition(formulaTextArea.getText());
                }

                oldJoinCondition.setCondition(liteCondition);
            }

            // peter:��Ҫreload parent
            DefaultTreeModel defaultTreeModel = (DefaultTreeModel) conditionsTree.getModel();
            ExpandMutableTreeNode parentTreeNode = (ExpandMutableTreeNode) selectedTreeNode.getParent();
            defaultTreeModel.reload(parentTreeNode);
            parentTreeNode.expandCurrentTreeNode(conditionsTree);
            conditionsTree.setSelectionPath(GUICoreUtils.getTreePath(selectedTreeNode));
        }
    }

    protected void swapNodesOfConditionTree(ExpandMutableTreeNode parentTreeNode, ExpandMutableTreeNode firstSelectTreeNode,
                                            ExpandMutableTreeNode secondTreeNode, int type) {
        int nextIndex = parentTreeNode.getIndex(firstSelectTreeNode);
        parentTreeNode.remove(firstSelectTreeNode);
        parentTreeNode.remove(secondTreeNode);

        parentTreeNode.insert(firstSelectTreeNode, nextIndex);
        parentTreeNode.insert(secondTreeNode, nextIndex);

        DefaultTreeModel treeModel = (DefaultTreeModel) conditionsTree.getModel();
        treeModel.reload(parentTreeNode);

        parentTreeNode.expandCurrentTreeNode(conditionsTree);
        if (type == MOVE_UP) {
            conditionsTree.setSelectionPath(GUICoreUtils.getTreePath(secondTreeNode));
        } else if (type == MOVE_DOWN) {
            conditionsTree.setSelectionPath(GUICoreUtils.getTreePath(firstSelectTreeNode));
        }


        // peter:���Button Enabled.
        checkButtonEnabledForList();
    }


    ActionListener radioActionListener = new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent evt) {
            applyCardsPane();
        }
    };

    private void applyCardsPane() {
        CardLayout cl = (CardLayout) (conditionCardPane.getLayout());
        if (this.commonRadioButton.isSelected()) {
            this.setBorder(GUICoreUtils.createTitledBorder(Inter.getLocText("Common_Condition"), null));
            cl.show(conditionCardPane, "DEFAULT");
        } else {
            this.setBorder(GUICoreUtils.createTitledBorder(Inter.getLocText("Formula_Condition"), null));
            cl.show(conditionCardPane, "FORMULA");
        }
    }

    /**
     * peter:���Button�Ƿ���Ա༭.
     */
    private void checkButtonEnabledForList() {
        modifyButton.setEnabled(false);
        removeButton.setEnabled(false);
        this.moveUpButton.setEnabled(false);
        this.moveDownButton.setEnabled(false);
        this.bracketButton.setEnabled(false);
        this.unBracketButton.setEnabled(false);

        TreePath selectedTreePath = conditionsTree.getSelectionPath();
        if (selectedTreePath != null) {
            modifyButton.setEnabled(true);
            removeButton.setEnabled(true);

            // peter:����ѡ�еĽڵ���Ƿ��ǵ�һ���������һ��.
            ExpandMutableTreeNode selectedTreeNode = (ExpandMutableTreeNode) selectedTreePath.getLastPathComponent();
            DefaultMutableTreeNode parentTreeNode = (DefaultMutableTreeNode) selectedTreeNode.getParent();
            if (parentTreeNode.getChildBefore(selectedTreeNode) != null) {
                moveUpButton.setEnabled(true);
            }
            if (parentTreeNode.getChildAfter(selectedTreeNode) != null) {
                moveDownButton.setEnabled(true);
            }

            // peter: ����ѡ���˳�����������,ͬʱû��ѡ�����еĽڵ�.
            int selectionCount = conditionsTree.getSelectionCount();
            if (selectionCount > 1 && parentTreeNode.getChildCount() > selectionCount) {
                this.bracketButton.setEnabled(true);
            }

            // peter:ѡ�еĽڵ������ListCondition,�ſ���ɾ������
            JoinCondition jonCondition = (JoinCondition) selectedTreeNode.getUserObject();
            Condition liteCondtion = jonCondition.getCondition();
            if (liteCondtion instanceof ListCondition) {
                this.unBracketButton.setEnabled(true);
            }
        }
    }

    /**
     * ��չ�¼�.
     */
    TreeExpansionListener treeExpansionListener = new TreeExpansionListener() {

        @Override
        public void treeExpanded(TreeExpansionEvent event) {
            TreePath selectedTreePath = event.getPath();
            if (selectedTreePath == null) {
                return;
            }

            ExpandMutableTreeNode selectedTreeNode = (ExpandMutableTreeNode) selectedTreePath.getLastPathComponent();
            selectedTreeNode.setExpanded(true);
        }

        @Override
        public void treeCollapsed(TreeExpansionEvent event) {
            TreePath selectedTreePath = event.getPath();
            if (selectedTreePath == null) {
                return;
            }

            ExpandMutableTreeNode selectedTreeNode = (ExpandMutableTreeNode) selectedTreePath.getLastPathComponent();
            selectedTreeNode.setExpanded(false);
        }
    };

    private ExpandMutableTreeNode getParentTreeNode() {
        DefaultTreeModel defaultTreeModel = (DefaultTreeModel) conditionsTree.getModel();
        TreePath selectedTreePath = conditionsTree.getSelectionPath();
        // peter:���û��ѡ��Ľڵ�,ֱ�����ӵ����ڵ�.
        ExpandMutableTreeNode parentTreeNode;
        if (selectedTreePath == null) {
            parentTreeNode = (ExpandMutableTreeNode) defaultTreeModel.getRoot();
        } else {
            parentTreeNode = (ExpandMutableTreeNode) ((ExpandMutableTreeNode) selectedTreePath.getLastPathComponent()).getParent();
        }
        // peter:���û��ѡ�еĽڵ�,ֱ�ӷ���.
        return parentTreeNode;
    }

    private boolean isExistedInParentTreeNode(ExpandMutableTreeNode parentTreeNode, JoinCondition newJoinCondition) {

        if (parentTreeNode == null) {
            return false;
        }
        JoinCondition parentJoinCondition = (JoinCondition) parentTreeNode.getUserObject();
        Condition parentLiteCondition = parentJoinCondition.getCondition();
        if (parentLiteCondition instanceof ListCondition) {
            // peter:������UserObject�Ľڵ�.

            for (int i = 0; i < parentTreeNode.getChildCount(); i++) {
                ExpandMutableTreeNode tempTreeNode = (ExpandMutableTreeNode) parentTreeNode.getChildAt(i);
                Object tempObject = tempTreeNode.getUserObject();
                if (tempObject instanceof JoinCondition) {
                    JoinCondition tempJoinCondition = (JoinCondition) tempObject;
                    if (ComparatorUtils.equals(tempJoinCondition, newJoinCondition)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private DefaultTreeCellRenderer conditionsTreeCellRenderer = new DefaultTreeCellRenderer() {

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row,
                                                      boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

            DefaultMutableTreeNode currentTreeNode = (DefaultMutableTreeNode) value;
            adjustParentListCondition(currentTreeNode);
            DefaultMutableTreeNode parentTreeNode = (DefaultMutableTreeNode) currentTreeNode.getParent();

            this.setIcon(null);
            JoinCondition joinCondition = (JoinCondition) currentTreeNode.getUserObject();
            StringBuilder sBuf = new StringBuilder();

            Condition liteCondition = joinCondition.getCondition();
            if (parentTreeNode != null && parentTreeNode.getFirstChild() != currentTreeNode) {
                if (joinCondition.getJoin() == DataConstants.AND) {
                    sBuf.append("and ");
                } else {
                    sBuf.append("or  ");
                }
            }

            if (liteCondition != null) {
                // TODO alex:����õ���liteConditionΪʲô����null��?
                sBuf.append(liteCondition.toString());
            }
            this.setText(sBuf.toString());

            return this;
        }
    };

    // peter:���ݺ��Ӽ���,������ǰ�ڵ��ListCondition��ֵ.
    protected void adjustParentListCondition(DefaultMutableTreeNode currentTreeNode) {
        DefaultMutableTreeNode parentTreeNode = (DefaultMutableTreeNode) currentTreeNode.getParent();

        Object userObj = currentTreeNode.getUserObject();
        if (userObj instanceof JoinCondition) {
            StringBuilder sBuf = new StringBuilder();

            JoinCondition joinCondition = (JoinCondition) userObj;
            Condition liteCondition = joinCondition.getCondition();
            if (parentTreeNode != null && parentTreeNode.getFirstChild() != currentTreeNode) {
                if (joinCondition.getJoin() == DataConstants.AND) {
                    sBuf.append("and ");
                } else {
                    sBuf.append("or  ");
                }
            }

            // peter:����ط���̬����ListCondition,��ΪListCondition�Ľڵ��仯��,
            // ���׽ڵ��ListCondition���UserObject��Ҫ���ű仯.
            if (liteCondition instanceof ListCondition) {
                ListCondition listCondition = (ListCondition) liteCondition;
                listCondition.clearJoinConditions();

                // peter:��̬���Ӻ��ӽڵ�
                int childCount = currentTreeNode.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    Object tmpUserObject = ((DefaultMutableTreeNode) currentTreeNode.getChildAt(i)).getUserObject();
                    if (tmpUserObject instanceof JoinCondition) {
                        listCondition.addJoinCondition((JoinCondition) tmpUserObject);
                    }
                }
            }
        }
    }

    /**
     * Sets whether or not this component is enabled.
     */
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        // checkenabled.
        checkButtonEnabledForList();
    }

    /**
     * Populate.
     *
     * @param liteCondition lite condition.
     */
    @Override
    public void populateBean(Condition liteCondition) {
    	if(liteCondition == null){
    		return;
    	}
        // peter: ��ɾ�����еĽڵ�
        DefaultTreeModel defaultTreeModel = (DefaultTreeModel) this.conditionsTree.getModel();
        ExpandMutableTreeNode rootTreeNode = (ExpandMutableTreeNode) defaultTreeModel.getRoot();
        rootTreeNode.setUserObject(new JoinCondition(DataConstants.AND, new ListCondition()));
        rootTreeNode.removeAllChildren();

        // peter:��Ҫ������ListCondition,���뵽����.
        if (liteCondition instanceof ListCondition) {
            ListCondition listCondition = (ListCondition) liteCondition;

            int joinConditionCount = listCondition.getJoinConditionCount();
            if (joinConditionCount == 0) {
                commonRadioButton.setSelected(true);
                applyCardsPane();

            }
            for (int i = 0; i < joinConditionCount; i++) {
                addLiteConditionToListCondition(rootTreeNode, listCondition.getJoinCondition(i));
            }
        } else if (needDoWithCondition(liteCondition)) {
            // peter:ֱ������
            ExpandMutableTreeNode newTreeNode = new ExpandMutableTreeNode(new JoinCondition(DataConstants.AND, liteCondition));
            rootTreeNode.add(newTreeNode);
        }

        // peter:��Ҫreload
        defaultTreeModel.reload(rootTreeNode);
        rootTreeNode.expandCurrentTreeNode(conditionsTree);
        // marks:Ĭ�ϵ�ѡ���һ��
        if (conditionsTree.getRowCount() > 0) {
            conditionsTree.setSelectionRow(0);
        }
        this.checkButtonEnabledForList();
        if (liteCondition == null) {
            try {
                defaultConditionPane.checkValid();
            } catch (Exception e) {//not need
            }
        }
    }

    protected boolean needDoWithCondition(Condition liteCondition) {
        return true;
    }

    // peter:���õݹ鷽ʽ,������ʼ�Ľڵ�
    private void addLiteConditionToListCondition(ExpandMutableTreeNode parentTreeNode, JoinCondition joinCondition) {
        ExpandMutableTreeNode newTreeNode = new ExpandMutableTreeNode(joinCondition);
        parentTreeNode.add(newTreeNode);

        // peter:��������.
        Condition liteCondition = joinCondition.getCondition();
        if (liteCondition instanceof ListCondition) {
            ListCondition listCondition = (ListCondition) liteCondition;

            int joinConditionCount = listCondition.getJoinConditionCount();
            for (int i = 0; i < joinConditionCount; i++) {
                addLiteConditionToListCondition(newTreeNode, listCondition.getJoinCondition(i));
            }
        }
    }


    /**
     * Update.
     *
     * @return the new lite condition.
     */
    @Override
    public Condition updateBean() {
        // Samuel���Ȱ�modifybutton
        modify();
        // peter: ��ɾ�����еĽڵ�
        DefaultTreeModel defaultTreeModel = (DefaultTreeModel) this.conditionsTree.getModel();
        ExpandMutableTreeNode rootTreeNode = (ExpandMutableTreeNode) defaultTreeModel.getRoot();

        int childCount = rootTreeNode.getChildCount();
        // peter: ���ֻ��һ�����ӽڵ�, ���ؿյ� ListCondition
        if (childCount == 0) {
            return new ListCondition();
        } // peter: ���roottreeNodeֻ��һ�����ӽڵ�.
        else if (childCount == 1) {
            JoinCondition joinCondition = (JoinCondition) ((ExpandMutableTreeNode) rootTreeNode.getChildAt(0)).getUserObject();
            return joinCondition.getCondition();
        } // peter: �кö�ĺ��ӽڵ�.
        else {
            // peter:��ȱ������еĺ��ӽڵ�
            Enumeration depthEnumeration = rootTreeNode.depthFirstEnumeration();
            while (depthEnumeration.hasMoreElements()) {
                this.adjustParentListCondition((ExpandMutableTreeNode) depthEnumeration.nextElement());
            }

            JoinCondition joinCondition = (JoinCondition) rootTreeNode.getUserObject();
            return joinCondition.getCondition();
        }
    }
}