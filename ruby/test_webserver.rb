#!/usr/bin/env ruby

require "rubygems"

begin
  require "#{File.realpath(File.dirname(__FILE__))}/../../../../Ruby/Gems/knjrbfw/lib/knjrbfw.rb"
rescue LoadError
  require "knjrbfw"
end

Knj.gem_require(:Http2)

Http2.new(:host => "localhost", :port => 8081) do |http|
  1.upto(5) do |rcount|
    res = http.get("")
    
    puts "Got the following body back:"
    puts res.body
  end
end