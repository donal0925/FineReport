package com.fr.design.chart.report;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.fr.base.Formula;
import com.fr.base.Utils;
import com.fr.chart.chartdata.BaseSeriesDefinition;
import com.fr.chart.chartdata.MapSingleLayerReportDefinition;
import com.fr.chart.chartdata.SeriesDefinition;
import com.fr.design.constants.UIConstants;
import com.fr.design.beans.FurtherBasicBeanPane;
import com.fr.design.event.UIObserver;
import com.fr.design.event.UIObserverListener;
import com.fr.design.formula.TinyFormulaPane;
import com.fr.design.gui.frpane.UICorrelationPane;
import com.fr.design.gui.ilable.UILabel;
import com.fr.design.gui.itable.UITableEditor;
import com.fr.design.gui.itextfield.UITextField;
import com.fr.general.Inter;
import com.fr.stable.StableUtils;

/**
 * ��ͼ ��Ԫ������ �����ͼ ����
 *
 * @author kunsnat E-mail:kunsnat@gmail.com
 * @version ����ʱ�䣺2012-10-22 ����05:16:23
 */
public class MapReportDataSinglePane extends FurtherBasicBeanPane<MapSingleLayerReportDefinition> implements UIObserver {

	private TinyFormulaPane areaNamePane;
	private UICorrelationPane seriesPane;
	private ArrayList<ChangeListener> changeListeners = new ArrayList<ChangeListener>();

	public MapReportDataSinglePane() {
		initCom();
	}

	private void initCom() {
		this.setLayout(new BorderLayout(0, 0));
		
		JPanel northPane = new JPanel();
		this.add(northPane, BorderLayout.NORTH);
		
		northPane.setLayout(new FlowLayout(FlowLayout.CENTER));
		
		northPane.add(new UILabel(Inter.getLocText("Area_Name") + ":", SwingConstants.RIGHT));
		areaNamePane = new TinyFormulaPane();
		areaNamePane.setPreferredSize(new Dimension(120, 20));
		northPane.add(areaNamePane);

		String[] columnNames = new String[]{Inter.getLocText(new String[]{"Filed", "Title"}), Inter.getLocText("Area_Value")};
		seriesPane = new UICorrelationPane(columnNames) {
			public UITableEditor createUITableEditor() {
				return new InnerTableEditor();
			}
		};

		this.add(seriesPane, BorderLayout.CENTER);
	}

	/**
	 * �������.
	 */
	public boolean accept(Object ob) {
		return true;
	}

	/**
	 * ����
	 */
	public void reset() {

	}

	/**
	 * ���浯������.
	 */
	public String title4PopupWindow() {
		return Inter.getLocText("Cell");
	}

	@Override
	public void populateBean(MapSingleLayerReportDefinition ob) {
		if(ob.getCategoryName() != null) {
			areaNamePane.populateBean(Utils.objectToString(ob.getCategoryName()));
			
			int size = ob.getTitleValueSize();
			List paneList = new ArrayList();
			for(int i = 0; i < size; i++) {
				BaseSeriesDefinition first = ob.getTitleValueWithIndex(i);
				if (first != null && first.getSeriesName() != null && first.getValue() != null) {
					paneList.add(new Object[]{first.getSeriesName(), first.getValue()});
				}
			}
			if (!paneList.isEmpty()) {
				seriesPane.populateBean(paneList);
			}
		}
	}

	@Override
	public MapSingleLayerReportDefinition updateBean() {
		MapSingleLayerReportDefinition reportDefinition = new MapSingleLayerReportDefinition();
		
		String areaName = areaNamePane.updateBean();
		if(StableUtils.canBeFormula(areaName)) {
			reportDefinition.setCategoryName(new Formula(areaName));
		} else {
			reportDefinition.setCategoryName(areaName);
		}

		List values = seriesPane.updateBean();
		if(values != null && !values.isEmpty() ) {
			for (int i = 0, size = values.size(); i < size; i++) {
				Object[] objects = (Object[]) values.get(i);
				Object name = objects[0];
				Object value = objects[1];
				
				if (StableUtils.canBeFormula(value)) {
					value = new Formula(Utils.objectToString(value));
				}
				SeriesDefinition definition = new SeriesDefinition(name, value);
				reportDefinition.addTitleValue(definition);
			}
		}
		
		return reportDefinition;
	}

	/**
	 * ������Ǽ�һ���۲��߼����¼�
	 *
	 * @param listener �۲��߼����¼�
	 */
	public void registerChangeListener(final UIObserverListener listener) {
		changeListeners.add(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				listener.doChange();
			}
		});
	}

	/**
	 * ����Ƿ���Ҫ��Ӧ���ӵĹ۲����¼�
	 *
	 * @return �����Ҫ��Ӧ�۲����¼��򷵻�true�����򷵻�false
	 */
	public boolean shouldResponseChangeListener() {
		return true;
	}

	private class InnerTableEditor extends UITableEditor {
		private JComponent editorComponent;

		/**
		 * ���ص�ǰ�༭����ֵ
		 */
		public Object getCellEditorValue() {
			if(editorComponent instanceof TinyFormulaPane) {
				return ((TinyFormulaPane)editorComponent).getUITextField().getText();
			} else if(editorComponent instanceof UITextField) {
				return ((UITextField)editorComponent).getText();
			}
			
			return super.getCellEditorValue();
		}

		/**
		 * ���ص�ǰ�༭��..
		 */
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
			if (column == table.getModel().getColumnCount()) {
				return null;
			}
			return getEditorComponent(column, value);
		}

		private JComponent getEditorComponent(int column, Object value) {
			if(column == 0) {
				UITextField field = new UITextField();
				addListener4UITextFiled(field);
				
				if(value != null) {
					field.setText(Utils.objectToString(value));
				}
				editorComponent = field;
			} else {
				TinyFormulaPane tinyPane = new TinyFormulaPane() {
					@Override
					public void okEvent() {
						seriesPane.stopCellEditing();
						seriesPane.fireTargetChanged();
					}
				};
				tinyPane.setBackground(UIConstants.FLESH_BLUE);
				
				addListener4UITextFiled(tinyPane.getUITextField());
				
				if(value != null) {
					tinyPane.getUITextField().setText(Utils.objectToString(value));
				}
				
				editorComponent = tinyPane;
			}
			return editorComponent;
		}
		
		private void addListener4UITextFiled(UITextField textField) {
			
			textField.addFocusListener(new FocusAdapter() {
				@Override
				public void focusLost(FocusEvent e) {
//					seriesPane.stopCellEditing();	//kunsnat: ��stop����Ϊ����ֱ�ӵ����ʽ�༭��ť, ������Ҫ������β��ܵ���.
					seriesPane.fireTargetChanged();
				}
			});
		}
	}
}