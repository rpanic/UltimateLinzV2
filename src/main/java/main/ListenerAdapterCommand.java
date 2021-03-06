package main;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.ListenerAdapterCommandException;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public abstract class ListenerAdapterCommand extends ListenerAdapter{

    protected String cmd;
    static String cmdPrefix = Main.INSTANCE.getPrefix();
    Message lastMessage;

    public ListenerAdapterCommand(String commandString) {
        if(commandString.startsWith(cmdPrefix)){
            commandString = commandString.replace(cmdPrefix, "");
        }
        this.cmd = commandString;
        this.lastMessage = null;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        super.onMessageReceived(event);

        MessageChannel channel = event.getChannel();
        String msg = event.getMessage().getContentRaw();

        if(!event.getJDA().getSelfUser().equals(event.getAuthor())){
            if(channel.getType() == ChannelType.GROUP || channel.getType() == ChannelType.PRIVATE || channel.getType() == ChannelType.TEXT){
                if(msg.startsWith(cmdPrefix + cmd)){

                    System.out.println("Command: " + event.getMessage().getContentDisplay() + " MessageId " + event.getMessageId());
                    lastMessage = event.getMessage();
                    try {

                        command(event, msg);

                    } catch (Exception e){
                        event.getChannel().sendMessage("Ein Fehler ist aufgetreten - bitte wende dich an Raphael").complete();
                        event.getChannel().sendMessage(Stream.concat(Stream.of(e.getMessage()), Stream.of(e.getStackTrace())).limit(7).map(x -> x.toString()).reduce((x, y) -> x + "\n" + y).orElse("No stack trace")).complete();
                    }
                }
            }
        }
    }

    private boolean command(MessageReceivedEvent event, String msg) {

        String[] tokens = msg.split(" ");

        if(tokens.length < 2){
            help(event, tokens);
            return true;
        }

        for(Method m : this.getClass().getMethods()){

            if(m.getName().equalsIgnoreCase(tokens[1])){

                boolean hasPermission;

                if(m.isAnnotationPresent(Permissioned.class)){

                    Permissioned permissioned = m.getAnnotation(Permissioned.class);

                    List<Role> roles = event.getJDA().getGuilds().get(0).getMember(event.getAuthor()).getRoles();
                    hasPermission = roles.stream()  //Guilds noch ordentlich machen...
                            .anyMatch(x ->
                                    Arrays.stream(permissioned.value()).anyMatch(y -> y.equalsIgnoreCase(x.getName()))
                            );
                }else{
                    hasPermission = true;
                }

                if(hasPermission) {
                    try {
                        if (m.isAnnotationPresent(Blocking.class)) {
                            new Thread(() -> {
                                try {
                                    m.invoke(this, event, tokens);
                                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                                    if(e instanceof InvocationTargetException){
                                        Throwable cause = e.getCause();
                                        if(cause instanceof ListenerAdapterCommandException){
                                            send(event.getChannel(), cause.getMessage());
                                        }
                                    }
                                    e.printStackTrace();
                                }
                            }).start();
                        } else {
                            m.invoke(this, event, tokens);
                        }
                        return true;
                    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                        //TODO, Command not found, help()
                        e.printStackTrace();
                    } catch (Exception e){  //TODO Test if that works
                        send(event.getChannel(), e.getMessage());
                    }
                }else{
                    send(event.getChannel(), "Für diesen Befehl hast du nicht die nötigen Berechtigungen");
                    return false;
                }
            }
        }

        send(event.getChannel(),
                "Dieser Befehl wurde leider nicht gefunden\n"+
                "Gib `" + cmdPrefix + this.cmd + "` ein, um die Hilfe anzuzeigen");

        return false;
    }

    public void help(MessageReceivedEvent event, String[] msg){

        MessageBuilder messageBuilder = new MessageBuilder();

        Help helpAnnotation = getClass().getAnnotation(Help.class);
        if(helpAnnotation != null){
            messageBuilder.append("|\n");
            messageBuilder.append(helpAnnotation.value());
        }

        messageBuilder.appendCodeBlock(getHelp(event.getJDA().getGuilds().get(0), event.getAuthor()), "");

        send(event.getChannel(), messageBuilder.build());
    }

    protected String getHelp(Guild guild, User user){
        int padding = 12;

        List<Method> methods = getCommandMethods(getClass(), guild, user);

        List<String> help = new ArrayList<>();

        methods.sort(Comparator.comparing(Method::getName));

        for(Method m : methods){

            Help helpAnnotation = m.getAnnotation(Help.class);
            String prefix = UtilsKt.padRight("-" + m.getName(), padding);
            if (helpAnnotation != null) {
                help.add(prefix + helpAnnotation.value());
            } else {
                help.add(prefix);
            }
        }

        return String.join("\n", help);
    }

    protected List<Method> getCommandMethods(Class<? extends ListenerAdapterCommand> clazz, Guild guild, User user){

        List<Role> roles = guild.getMember(user).getRoles();

        List<Method> methods = Arrays.stream(clazz.getMethods())
                .filter(x -> !x.getDeclaringClass().getName().equals(ListenerAdapterCommand.class.getName()) && ListenerAdapterCommand.class.isAssignableFrom(x.getDeclaringClass()))
                .filter(x -> x.getParameterCount() > 0)
                .filter(x -> x.getParameterTypes()[0].isAssignableFrom(MessageReceivedEvent.class))
                .filter(m -> {
                    Permissioned permissioned = m.getAnnotation(Permissioned.class);
                    if(permissioned != null) {
                        return roles.stream().anyMatch(x ->
                                Arrays.stream(permissioned.value()).anyMatch(y -> y.equalsIgnoreCase(x.getName())));
                    }else{
                        return true;
                    }
                })
                .collect(Collectors.toList());

        return methods;
    }

    public void send(MessageChannel c, String msg){
        c.sendMessage(msg).submit();
    }

    public void send(MessageChannel messageChannel, Message message){
        messageChannel.sendMessage(message).submit();
    }

    public Message sendSync(MessageChannel c, String msg) {
        return c.sendMessage(msg).complete();
    }

//    public void deleteCommandAfter(long millis) {
//        MessageTimer.deleteAfter(lastMessage, millis);
//    }

}