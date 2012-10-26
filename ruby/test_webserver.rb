#!/usr/bin/env ruby

require "rubygems"

begin
  require "#{File.realpath(File.dirname(__FILE__))}/../../../../Ruby/Gems/knjrbfw/lib/knjrbfw.rb"
rescue LoadError
  require "knjrbfw"
end

Knj.gem_require(:Http2)

Http2.new(:host => "localhost", :port => 8081, :debug => false) do |http|
  1.upto(5) do |rcount|
    puts "Getting result."
    res = http.get("")
    
    puts "Got the following body back:"
    puts res.body
  end
end