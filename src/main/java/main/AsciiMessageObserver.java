package main;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;

public class AsciiMessageObserver {

    AsciiTable<String> table;

    public AsciiMessageObserver(Message message){

        table = new AsciiTable<String>(message, columns());

    }

    private AsciiTable.ColumnDefinition<String> columns() {

        return new AsciiTable.ColumnDefinition<String>()
                .addPrimaryColumn("Name")
                .addColumn("In", x -> x.equalsIgnoreCase("in"))
                .addColumn("Out", x -> x.equalsIgnoreCase("out"));

    }

    public void answerChanged(String name, String choice){

        table.data(name, choice);

        editMessage();

    }

    public void editMessage(){

        Message newmessage = new MessageBuilder()
                .appendCodeBlock(table.renderAscii(), "")
                .build();

        table.getTableMessage().editMessage(newmessage).complete();
    }
}