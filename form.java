import java.awt.*;
import java.applet.*;

class exp extends Frame
{
	Button b;
	Label l1,l2;
	TextField t1,t2;

	exp()
	{
		b = new Button("Submit");
		l1 = new Label("Username");
		l2 = new Label("Password");
		t1 = new TextField();
		t2 = new TextField();
		add(l1);
		add(t1);
		add(l2);
		add(t2);
		add(b);
		l1.setBounds(40,50,80,30);
		l2.setBounds(40,100,80,30);
		t1.setBounds(150,50,100,30);
		t2.setBounds(150,100,100,30);
		b.setBounds(60,200,80,30);
		setLayout(null);
		setSize(500,500);
		setVisible(true); 
	}
}
class form
{
	public static void main(String[] args)
	{	
		new exp();
	}
}