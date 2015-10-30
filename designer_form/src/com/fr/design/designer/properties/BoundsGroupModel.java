package com.fr.design.designer.properties;

import java.awt.Rectangle;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import com.fr.general.Inter;
import com.fr.design.mainframe.widget.editors.IntegerPropertyEditor;
import com.fr.design.mainframe.widget.editors.PropertyCellEditor;
import com.fr.design.designer.beans.ConstraintsGroupModel;
import com.fr.design.designer.creator.XCreator;
import com.fr.design.designer.creator.XWAbsoluteLayout;
import com.fr.form.ui.FreeButton;
import com.fr.form.ui.container.WAbsoluteLayout;

/**
 * ���Բ���ʱ���������
 */
public class BoundsGroupModel implements ConstraintsGroupModel {
    private static final int MINHEIGHT = 21;
    private static final int FOUR = 4;

    private DefaultTableCellRenderer renderer;
    private PropertyCellEditor editor;
    private XCreator component;
    private XWAbsoluteLayout parent;

    public BoundsGroupModel(XWAbsoluteLayout parent, XCreator comp) {
        this.parent = parent;
        component = comp;
        renderer = new DefaultTableCellRenderer();
        editor = new PropertyCellEditor(new IntegerPropertyEditor());
    }

    @Override
    public String getGroupName() {
        return Inter.getLocText("Form-Component_Bounds");
    }

    @Override
    public int getRowCount() {
        return FOUR;
    }

    @Override
    public TableCellRenderer getRenderer(int row) {
        return renderer;
    }

    @Override
    public TableCellEditor getEditor(int row) {
        return editor;
    }

    @Override
    public Object getValue(int row, int column) {
        if (column == 0) {
            switch (row) {
                case 0:
                    return Inter.getLocText("X-Coordinate");
                case 1:
                    return Inter.getLocText("Y-Coordinate");
                case 2:
                    return Inter.getLocText("Widget-Width");
                default:
                    return Inter.getLocText("Widget-Height");
            }
        } else {
            switch (row) {
                case 0:
                    return component.getX();
                case 1:
                    return component.getY();
                case 2:
                    return component.getWidth();
                default:
                    return component.getHeight();
            }
        }
    }

    @Override
    public boolean setValue(Object value, int row, int column) {
        if (column == 1) {
        	int v = value == null ? 0 : ((Number) value).intValue();
            Rectangle bounds = new Rectangle(component.getBounds());
			switch (row) {
			case 0:
				if (bounds.x == v){
					return false;
                }
				bounds.x = v;
				break;
			case 1:
				if (bounds.y == v){
					return false;
                }
				bounds.y = v;
				break;
			case 2:
				if (bounds.width == v){
					return false;
                }
				bounds.width = v;
				break;
			case 3:
                if(v < MINHEIGHT){
                    JOptionPane.showMessageDialog(null, Inter.getLocText("Min-Height") + "21");
                    v = component.getHeight();
                }
				if (bounds.height == v){
					return false;
                }
				bounds.height = v;
				break;
			}
            WAbsoluteLayout wabs = parent.toData();
            wabs.setBounds(component.toData(),bounds);
            component.setBounds(bounds);
            
            return true;
        } else {
            return false;
        }
    }

	@Override
	public boolean isEditable(int row) {
        boolean flag = (component.toData()) instanceof FreeButton && ((FreeButton) component.toData()).isCustomStyle();
		if ((row == 2 || row == 3)
				&& flag) {
			// �Զ���Button��ʽ��Ӧ�ÿ������ø߶ȣ����ȣ����Ȳ�Ϊ���������XCreator�Ӹ��Ƿ�������õķ�����
			return false;
		}
		return true;
	}
}