package com.darkona.tardigrade.configuration;

import org.apache.commons.cli.*;

import java.util.Arrays;

public class TardigradeConfiguration {

    private final CommandLine cmd;

    private boolean enableColor = true;
    private boolean enableHeader = true;
    private boolean enableBody= true;
    public boolean headers(){ return enableHeader; }
    public boolean body(){ return enableBody; }
    public String input() {
        return cmd.getOptionValue("i", "in");
    }

    public String output() {
        return cmd.getOptionValue("o", "out");
    }

    public String port() {
        return cmd.getOptionValue("p", "80");
    }

    public String params() { return cmd.getOptionValue("a", ""); }

    public boolean quiet() {return cmd.hasOption("q"); }

    private void addOption(Options o, String s){
        var ss = s.split(",");
        o.addOption(Option.builder(ss[0].trim()).longOpt(ss[1].trim()).desc(ss[2].trim()).numberOfArgs(1).build());
    }
    public TardigradeConfiguration(String[] args) throws ParseException {
        Options options = new Options();

        addOption(options,"p, port, Specify server port.");

        options.addOption(Option.builder("p").longOpt("port").desc("Server port.").type(Integer.class).numberOfArgs(1).build());
        options.addOption(Option.builder("o").longOpt("output").desc("Output directory for writing files").numberOfArgs(1).build());
        options.addOption(Option.builder("i").longOpt("input").desc("Input directory for loading files").numberOfArgs(1).build());
        options.addOption(Option.builder("q").longOpt("quiet").desc("Quiet mode, no console output.").build());
        options.addOption(Option.builder("d").longOpt("disable").desc("Disable features.").hasArgs().build());


        cmd = new DefaultParser().parse(options, args);
        System.out.println(params());
        if(cmd.hasOption("d")){
            var kwargs = cmd.getOptionValues("d");
            for(String arg : kwargs){
                if ("color".equalsIgnoreCase(arg)) {
                    enableColor = false;
                    break;
                }
                if("header".equalsIgnoreCase(arg)){
                    enableHeader = false;
                }
                if("body".equalsIgnoreCase(arg)){
                    enableBody = false;
                }
            }
        }
    }


    public boolean color() {
        return enableColor;
    }

    public void setColor(boolean enable){
        this.enableColor = enable;
    }
}
