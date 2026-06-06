# DataLib Engine - Init
# This function runs on world load via minecraft:load tag
# Sets up the datalib:engine storage with loaded:1b

data modify storage datalib:engine loaded set value 1b
scoreboard objectives add datalib.loaded dummy
scoreboard players set #engine datalib.loaded 1

tellraw @a[permission_level=2] {"text":"[DataLib] ","color":"aqua","extra":[{"text":"Engine loaded.","color":"white"}]}

function datalib:check_all
