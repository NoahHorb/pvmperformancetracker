package net.runelite.client.plugins.pvmperformancetracker.enums;

/**
 * NPC IDs for all NPCs with more than one attack style or more than one max hit type.
 *
 * Auto-generated from npc-database.json via query_npcs.py
 * Entries with null/removed IDs are noted as comments.
 *
 * Using an interface (not enum) so constants resolve directly to int[] without .id
 * e.g. for (int id : eNPC.VARDORVIS) { ... }
 *
 * for bosses with different mechanics for different variants, come in and manually curate
 */
public interface eNPC
{
    // -----------------------------------------------------------------------
    // A
    // -----------------------------------------------------------------------
    int[] ABERRANT_SPECTRE                                 = {2, 3, 4, 5, 6, 7};
    int[] ABOMINATION                                      = {8260, 8261, 8262};
    int[] ABYSSAL_GUARDIAN                                 = {2585};
    int[] ABYSSAL_SIRE                                     = {5886, 5887, 5888, 5889, 5890, 5891, 5908};
    int[] ADAMANT_DRAGON                                   = {8030, 8090};
    int[] AGRITH_NAAR                                      = {911, 6388};
    int[] AGRITH_NAAR_HARD                                 = {6327};
    int[] AGRITH_NA_NA                                     = {4880, 6369};
    int[] AGRITH_NA_NA_HARD                                = {6308};
    int[] AKKHA                                            = {11789, 11790, 11791, 11792, 11793, 11794, 11795, 11796};
    int[] ALCHEMICAL_HYDRA                                 = {8615, 8616, 8617, 8618, 8619, 8620, 8621, 8622};
    int[] AMOXLIATL                                        = {13685};
    int[] ANCIENT_WYVERN                                   = {7795};
    int[] ANCIENT_ZYGOMITE                                 = {7797};
    int[] ARAXXOR_IN_COMBAT                                = {13668};
    int[] ARRG                                             = {642, 643, 6392};
    int[] ARRG_HARD                                        = {6331};
    int[] ARTIO                                            = {11992};
    int[] ASSASSIN                                         = {10940, 10941, 12062, 13514, 13515};

    // -----------------------------------------------------------------------
    // B
    // -----------------------------------------------------------------------
    int[] BALANCE_ELEMENTAL                                = {13528, 13529, 13530};
    int[] BANSHEE                                          = {414};
    int[] BARRELCHEST                                      = {600};
    int[] BASILISK_KNIGHT                                  = {9293};
    int[] BASILISK_SENTINEL                                = {9258};
    int[] BA_BA                                            = {11778, 11779, 11780};
    int[] BLACK_DRAGON                                     = {252, 253, 254, 255, 256, 257, 258, 259, 7861, 7862, 7863, 8084, 8085};
    int[] BLOODTHIRSTY_BASILISK_SENTINEL                   = {12566};
    int[] BLOODTHIRSTY_BLOODVELD                           = {12568};
    int[] BLOODTHIRSTY_DRAKE                               = {12586};
    int[] BLOODTHIRSTY_HYDRA                               = {12588};
    int[] BLOODTHIRSTY_JELLY                               = {12570};
    int[] BLOODTHIRSTY_MUTATED_BLOODVELD                   = {12569};
    int[] BLOODTHIRSTY_NIGHT_BEAST                         = {12580};
    int[] BLOODTHIRSTY_SHADOW_WYRM                         = {12584, 12585};
    int[] BLOODTHIRSTY_SMOKE_DEVIL                         = {12577};
    int[] BLOODTHIRSTY_WARPED_JELLY                        = {12571};
    int[] BLOODVELD                                        = {484, 485, 486, 487, 3138};
    int[] BLUE_DRAGON_NORMAL                               = {265, 266, 267, 268, 269};
    int[] BLUE_DRAGON_RUINS_OF_TAPOYAUIK                   = {14103, 14104};
    int[] BLUE_DRAGON_TASK_ONLY                            = {5878, 5879, 5880, 5881, 5882};
    int[] BRANDA_THE_FIRE_QUEEN                            = {12596};
    int[] BRONZE_DRAGON                                    = {270, 271, 7253};
    int[] BRUTAL_BLACK_DRAGON                              = {7275, 8092, 8093};
    int[] BRUTAL_BLUE_DRAGON                               = {7273, 13795};
    int[] BRUTAL_GREEN_DRAGON                              = {2918};
    int[] BRUTAL_RED_DRAGON                                = {7274, 8087};
    int[] BRYOPHYTA                                        = {8195};

    // -----------------------------------------------------------------------
    // C
    // -----------------------------------------------------------------------
    int[] CALLISTO                                         = {6503, 6609};
    int[] CALVAR_ION                                       = {11993, 11994};
    int[] CATABLEPON                                       = {2474, 2475, 2476};
    int[] CAVE_ABOMINATION                                 = {7401};
    int[] CAVE_HORROR                                      = {1047, 1048, 1049, 1050, 1051};
    int[] CERBERUS                                         = {5862, 5863, 5866};
    int[] CHAMPION_OF_SCABARAS                             = {11482, 11483};
    int[] CHAOS_DRUID                                      = {520};
    int[] CHAOS_ELEMENTAL                                  = {2054, 6505};
    int[] CHILLED_JELLY                                    = {13799};
    int[] CHOKE_DEVIL                                      = {7404};
    int[] COLOSSAL_HYDRA                                   = {10402};
    int[] COMMANDER_ZILYANA                                = {2205, 6493};
    int[] CORPOREAL_BEAST                                  = {319};
    int[] CORRUPTED_HUNLLEF                                = {9035, 9036, 9037, 9038};
    int[] CORRUPT_LIZARDMAN                                = {7997, 8000};
    int[] CORRUPT_LIZARDMAN_HARD                           = {7996};
    int[] CRAZY_ARCHAEOLOGIST                              = {6618};
    int[] CRYSTALLINE_HUNLLEF                              = {9021, 9022, 9023, 9024};
    int[] CULINAROMANCER                                   = {3400, 4849, 4872, 4873, 4874, 4875, 4876, 4877, 4878, 4879};

    // -----------------------------------------------------------------------
    // D
    // -----------------------------------------------------------------------
    int[] DAGANNOTH_MOTHER                                 = {980, 981, 982, 983, 984, 985, 986, 987, 988, 6361, 6362, 6363, 6364, 6365, 6366};
    int[] DAGANNOTH_MOTHER_HARD                            = {6300, 6301, 6302, 6303, 6304, 6305};
    int[] DAMIEN_LEUCURTE                                  = {9561, 9562, 9563, 9564};
    int[] DARK_BEAST                                       = {4005, 7250};
    int[] DAWN                                             = {7852, 7853, 7884, 7885};
    int[] DEMONIC_GORILLA                                  = {7144, 7145, 7146, 7147, 7148, 7149, 7152};
    int[] DERANGED_ARCHAEOLOGIST                           = {7806};
    int[] DERWEN                                           = {7513, 7859, 12450};
    int[] DESSOURT                                         = {4883, 6372};
    int[] DESSOURT_HARD                                    = {6311};
    int[] DESSOUS                                          = {3459, 3460, 6344};
    int[] DESSOUS_HARD                                     = {1128};
    int[] DEVIANT_SPECTRE                                  = {7279};
    int[] DHAROK_THE_WRETCHED                              = {1673};
    int[] DON_T_KNOW_WHAT                                  = {8439};
    int[] DOOM_OF_MOKHAIOTL_BURROWED                       = {14709};
    int[] DOOM_OF_MOKHAIOTL_NORMAL                         = {14707};
    int[] DOOM_OF_MOKHAIOTL_SHIELDED                       = {14708};
    int[] DRAKE                                            = {8612, 8613};
    int[] DREADBORN_ARAXYTE                                = {13680};
    int[] DRUID                                            = {3258};
    int[] DUKE_SUCELLUS_AWAKENED                           = {12166, 12167, 12191, 12192, 12196};
    int[] DUKE_SUCELLUS_POST_QUEST                         = {12166, 12167, 12191, 12192, 12196};
    int[] DUKE_SUCELLUS_QUEST                              = {12193, 12194, 12195, 12196};
    int[] DURIAL321                                        = {13662};
    int[] DUSK                                             = {7851, 7854, 7855, 7882, 7883, 7886, 7887, 7888, 7889};
    int[] DUST_DEVIL                                       = {423, 7249, 11238};

    // -----------------------------------------------------------------------
    // E
    // -----------------------------------------------------------------------
    int[] ELDER_AQUANITE                                   = {15502, 15503};
    int[] ELDER_CUSTODIAN_STALKER                          = {14704};
    int[] ELDRIC_THE_ICE_KING                              = {14147};
    int[] ELIDINIS_WARDEN_PHASE_2                          = {11753, 11754, 11755};
    int[] ELIDINIS_WARDEN_PHASE_3                          = {11761, 11763};
    int[] ELVARG                                           = {817, 6349};
    int[] ELVARG_HARD                                      = {6118};
    int[] ESSYLLT                                          = {8847, 8871, 8872, 8950, 9016, 9249};
    int[] ESSYLLT_HARD                                     = {9247};
    int[] EXPERIMENT_NO_2                                  = {5126};

    // -----------------------------------------------------------------------
    // F
    // -----------------------------------------------------------------------
    int[] FAREED                                           = {3456, 6348};
    int[] FAREED_HARD                                      = {4115};
    int[] FEVER_SPIDER                                     = {626};
    int[] FLAMBEED                                         = {4881, 6370};
    int[] FLAMBEED_HARD                                    = {6309};
    int[] FLAMING_PYRELORD                                 = {7394};
    int[] FRAGMENT_OF_SEREN                                = {8917, 8918, 8919, 8920};
    int[] FROST_DRAGON                                     = {14922};

    // -----------------------------------------------------------------------
    // G
    // -----------------------------------------------------------------------
    int[] GALVEK_MONSTER                                   = {8094, 8095, 8096, 8097, 8098, 8177, 8178, 8179};
    int[] GELATINNOTH_MOTHER                               = {4884, 4885, 4886, 4887, 4888, 4889, 6373, 6374, 6375, 6376, 6377, 6378};
    int[] GELATINNOTH_MOTHER_HARD                          = {6312, 6313, 6314, 6315, 6316, 6317};
    int[] GENERAL_GRAARDOR                                 = {2215, 6494};
    int[] GHOUL_CHAMPION                                   = {3330};
    int[] GIANT_ROC                                        = {763, 6384};
    int[] GIANT_ROC_HARD                                   = {6323};
    int[] GIANT_SCARAB                                     = {797, 798, 6343};
    int[] GIANT_SCARAB_HARD                                = {1127};
    int[] GIANT_SEA_SNAKE                                  = {1101};
    int[] GLOUGH                                           = {7101, 7102, 7103};
    int[] GNOME_LEVEL_1                                    = {5969, 6094, 6095, 6096};
    int[] GNOME_LEVEL_3                                    = {5968, 5970};
    int[] GREAT_OLM_HEAD                                   = {7551, 7554};
    int[] GREEN_DRAGON                                     = {260, 261, 262, 263, 264, 7868, 7869, 7870, 8073, 8076, 8082};
    int[] GUARDIAN_DRAKE                                   = {10400};

    // -----------------------------------------------------------------------
    // H
    // -----------------------------------------------------------------------
    int[] HEADLESS_BEAST                                   = {10493, 10506};
    int[] HEADLESS_BEAST_HARD                              = {10492};
    int[] HESPORI                                          = {8583, 11192};
    int[] HOLE_IN_THE_WALL                                 = {475};
    int[] HOPELESS_CREATURE                                = {1072, 1073, 1074};
    int[] HYDRA                                            = {8609};

    // -----------------------------------------------------------------------
    // I
    // -----------------------------------------------------------------------
    int[] ICEFIEND                                         = {3140, 4813};
    int[] ICE_TROLL_KING                                   = {5822, 6356};
    int[] IMP_CHAMPION                                     = {3355};
    int[] INFERNAL_PYRELORD                                = {9465};
    int[] INSATIABLE_BLOODVELD                             = {7397};
    int[] INSATIABLE_MUTATED_BLOODVELD                     = {7398};
    int[] INVRIGAR_THE_NECROMANCER                         = {511};
    int[] IRON_DRAGON                                      = {272, 273, 7254, 8080};

    // -----------------------------------------------------------------------
    // J
    // -----------------------------------------------------------------------
    int[] JALTOK_JAD                                       = {7700, 7704, 10623};
    int[] JAL_AK                                           = {7693};
    int[] JAL_XIL                                          = {7698, 7702};
    int[] JAL_ZEK                                          = {7699, 7703};
    int[] JAVELIN_COLOSSUS                                 = {12817};
    int[] JELLY_REGULAR                                    = {437, 438, 439, 440, 441, 442};
    int[] JELLY_WILDERNESS_SLAYER_CAVE                     = {11241, 11242, 11243, 11244, 11245};
    int[] JUDGE_OF_YAMA                                    = {10936, 10938, 14180};
    int[] JUNGLE_DEMON                                     = {1443, 6382};
    int[] JUSTICIAR_ZACHARIAH                              = {5977, 7858, 12449};
    int[] JUVENILE_CUSTODIAN_STALKER                       = {14702};

    // -----------------------------------------------------------------------
    // K
    // -----------------------------------------------------------------------
    int[] KALPHITE_QUEEN                                   = {963, 965, 4303, 4304, 6500, 6501};
    int[] KAMIL                                            = {3458, 6345};
    int[] KAMIL_HARD                                       = {1129};
    int[] KARAMEL                                          = {4882, 6371};
    int[] KARAMEL_HARD                                     = {6310};
    int[] KASONDE                                          = {12262, 12263};
    int[] KASONDE_THE_CRAVEN                               = {12331, 12332};
    int[] KET_ZEK                                          = {3125, 3126};
    int[] KILLERWATT                                       = {469, 470};
    int[] KING_BLACK_DRAGON                                = {239, 2642, 6502, 12440};
    int[] KNIGHT_OF_ARDOUGNE_EAST_ARDOUGNE                 = {3297, 11936};
    int[] KOB                                              = {7106, 7107};
    int[] KREE_ARRA                                        = {3162, 6492};
    int[] KROY                                             = {9560};
    int[] KRUK                                             = {6805};
    int[] K_RIL_TSUTSAROTH                                 = {3129, 6495};

    // -----------------------------------------------------------------------
    // L
    // -----------------------------------------------------------------------
    int[] LAVA_DRAGON                                      = {6593};
    int[] LAVA_STRYKEWYRM                                  = {15500};
    int[] LESSER_DEMON_CHAMPION                            = {3357};
    int[] LIZARDMAN                                        = {6914, 6915, 6916, 6917, 8563};
    int[] LIZARDMAN_BRUTE                                  = {6918, 6919, 8564, 10947};
    int[] LIZARDMAN_SHAMAN                                 = {6766, 6767, 7573, 7574, 7744, 7745, 8565};
    int[] LONG_TAILED_WYVERN                               = {7792};
    int[] LUCIUS                                           = {14363};

    // -----------------------------------------------------------------------
    // M
    // -----------------------------------------------------------------------
    int[] MAGMA_STRYKEWYRM                                 = {15504};
    int[] MANTICORE                                        = {12818};
    int[] MARBLE_GARGOYLE                                  = {7407, 7408};
    int[] MATURE_CUSTODIAN_STALKER                         = {14703};
    int[] MELZAR_THE_MAD                                   = {823};
    int[] MENAPHITE_SHADOW                                 = {11462};
    int[] MINOTAUR                                         = {13814, 13815};
    int[] MITHRIL_DRAGON                                   = {2919, 8088, 8089};
    int[] MONK_OF_ZAMORAK                                  = {527, 528, 529, 3484, 3485, 3486, 8400, 8401, 8698};
    int[] MOSSCHIN                                         = {11271, 11298};
    int[] MOSS_GIANT                                       = {8736};
    int[] MOSS_GUARDIAN                                    = {891, 6386};
    int[] MOSS_GUARDIAN_HARD                               = {6325};
    int[] MOTHER                                           = {8428, 8429, 8430};
    int[] MUTANT_TARN                                      = {6477};
    int[] MUTATED_BLOODVELD                                = {7276};
    int[] MUTATED_TERRORBIRD                               = {12464};
    int[] MUTTADILE                                        = {7561, 7562, 7563};

    // -----------------------------------------------------------------------
    // N
    // -----------------------------------------------------------------------
    int[] NECROMANCER                                      = {1025};
    int[] NEX                                              = {11278, 11279, 11280, 11281, 11282};
    int[] NEZIKCHENED                                      = {3962};
    int[] NIGHT_BEAST                                      = {7409, 12459};
    int[] NPC_13785_PRINCE_ITZLA_ARKAN                     = {13784};
    int[] NUCLEAR_SMOKE_DEVIL                              = {7406};
    int[] NYLOCAS_VASILIAS                                 = {8354, 8355, 8356, 8357, 10786, 10787, 10788, 10789, 10807, 10808, 10809, 10810};

    // -----------------------------------------------------------------------
    // O
    // -----------------------------------------------------------------------
    int[] OBOR                                             = {7416};

    // -----------------------------------------------------------------------
    // P
    // -----------------------------------------------------------------------
    int[] PARASITE                                         = {9452, 9453, 9468, 9469};
    int[] PENANCE_QUEEN                                    = {5775};
    int[] PESTILENT_BLOAT                                  = {8359, 10812, 10813};
    int[] PHANTOM_MUSPAH                                   = {12077, 12079, 12080};
    int[] PHOSANI_S_NIGHTMARE                              = {9416, 9417, 9418, 9419, 9420, 9421, 9422, 9424, 11153, 11154, 11155};
    int[] PORAZDIR                                         = {7515, 7860, 12448};
    int[] POSSESSED_PRIEST                                 = {4210};
    int[] PYREFIEND                                        = {433, 434, 435, 436, 3139};
    int[] PYRELORD                                         = {6762, 6795};

    // -----------------------------------------------------------------------
    // R
    // -----------------------------------------------------------------------
    int[] RANIS_DRAKAN                                     = {8242, 8243, 8244, 8245, 8246, 8247};
    int[] REANIMATED_BLOODVELD                             = {7034};
    int[] REANIMATED_DRAGON                                = {7039};
    int[] REDEYES                                          = {11272, 11299};
    int[] RED_DRAGON                                       = {247, 248, 249, 250, 251, 8075, 8078, 8079, 14511};
    int[] REVENANT_CYCLOPS                                 = {7934};
    int[] REVENANT_DARK_BEAST                              = {7938};
    int[] REVENANT_DEMON                                   = {7936};
    int[] REVENANT_DRAGON                                  = {7940};
    int[] REVENANT_GOBLIN                                  = {7931};
    int[] REVENANT_HELLHOUND                               = {7935};
    int[] REVENANT_HOBGOBLIN                               = {7933};
    int[] REVENANT_IMP                                     = {7881};
    int[] REVENANT_KNIGHT                                  = {7939};
    int[] REVENANT_MALEDICTUS                              = {11246};
    int[] REVENANT_ORK                                     = {7937};
    int[] REVENANT_PYREFIEND                               = {7932};
    int[] RUNE_DRAGON                                      = {8027, 8031, 8091};

    // -----------------------------------------------------------------------
    // S
    // -----------------------------------------------------------------------
    int[] SARACHNIS                                        = {8713};
    int[] SARADOMIN_WIZARD                                 = {2955};
    int[] SCION                                            = {5918, 6177};
    int[] SCREAMING_BANSHEE                                = {7390};
    int[] SCREAMING_TWISTED_BANSHEE                        = {7391};
    int[] SCURRIUS                                         = {7221, 7222};
    int[] SEA_TROLL_QUEEN                                  = {4315};
    int[] SHADOW_WYRM                                      = {10398, 10399};
    int[] SHELLBANE_GRYPHON                                = {14860, 15010};
    int[] SKELETAL_WYVERN                                  = {465, 466, 467, 468};
    int[] SKELETON                                         = {6441};
    int[] SKELETON_MAGE                                    = {84};
    int[] SKOTIZO                                          = {7286};
    int[] SLASH_BASH                                       = {882};
    int[] SMOKE_DEVIL                                      = {498};
    int[] SNAILFEET                                        = {11270, 11275};
    int[] SOL_HEREDIT                                      = {12821};
    int[] SOTETSEG                                         = {8387, 8388, 10864, 10865, 10867, 10868};
    int[] SOURHOG                                          = {10435, 10436};
    int[] SPAWN                                            = {5916, 5917};
    int[] SPINDEL                                          = {11998};
    int[] SPINOLYP                                         = {5947, 5961, 5963};
    int[] SPITTING_WYVERN                                  = {7794};
    int[] STEEL_DRAGON                                     = {139, 274, 275, 7255};
    int[] STRANGE_CREATURE                                 = {12073, 12075, 12076, 12081};  //secret of north
    int[] STRANGE_CREATURE2                                 = {14706};      //other random quest
    int[] STRONGBONES                                      = {11273, 11300};
    int[] SUQAH                                            = {787, 788, 789, 790, 791, 792, 793};

    // -----------------------------------------------------------------------
    // T
    // -----------------------------------------------------------------------
    int[] TALONED_WYVERN                                   = {7793};
    int[] TEKTON                                           = {7540, 7541, 7542, 7545};
    int[] TEKTON_ENRAGED                                   = {7543, 7544};
    int[] THE_FORSAKEN_ASSASSIN                            = {12328};
    int[] THE_HUEYCOATL_HEAD                               = {14009, 14010, 14011, 14012, 14013};
    int[] THE_INADEQUACY                                   = {3473, 6350};
    int[] THE_INADEQUACY_HARD                              = {6119};
    int[] THE_JORMUNGAND                                   = {9290, 9291};
    int[] THE_LEVIATHAN                                    = {12214, 12215, 12219};
    int[] THE_MIMIC                                        = {8633};
    int[] THE_NIGHTMARE                                    = {9425, 9426, 9427, 9428, 9429, 9430, 9431, 9432, 9433, 9460};
    int[] THE_WHISPERER                                    = {12204, 12205, 12206, 12207};
    int[] THROWER_TROLL                                    = {931, 932, 933, 934, 935};
    int[] TOK_XIL                                          = {141, 2193, 2194, 3121, 3122};
    int[] TOLNA                                            = {1075, 1077};
    int[] TORCHER                                          = {1714, 1715, 1716, 1717, 1718, 1719, 1720, 1721, 1722, 1723};
    int[] TORTOISE                                         = {6076};
    int[] TORTURED_GORILLA                                 = {7150, 7151, 7153};
    int[] TREUS_DAYTH                                      = {3616};
    int[] TUMEKEN_S_WARDEN_PHASE_2                         = {11756, 11757, 11758};
    int[] TUMEKEN_S_WARDEN_PHASE_3                         = {11762, 11764};
    int[] TWISTED_BANSHEE                                  = {7272};
    int[] TYPHOR                                           = {9295, 9296};
    int[] TZHAAR_MEJ                                       = {2154, 2155, 2156, 2157, 2158, 2159, 2160};
    int[] TZHAAR_XIL                                       = {2169, 2172};
    int[] TZKAL_ZUK                                        = {7706};
    int[] TZTOK_JAD                                        = {3127, 6506, 13661};
    int[] TZTOK_JAD_REK                                    = {15557};

    // -----------------------------------------------------------------------
    // U
    // -----------------------------------------------------------------------
    int[] UNDEAD_DRUID                                     = {2145};
    int[] UNGADULU                                         = {3957, 3958};

    // -----------------------------------------------------------------------
    // V
    // -----------------------------------------------------------------------
    int[] VARDORVIS                                        = {12223, 12224, 12228, 12425, 12426};
    int[] VENENATIS                                        = {6504, 6610};
    int[] VERZIK_VITUR_ENTRY                               = {10833, 10834, 10835, 10836};
    int[] VERZIK_VITUR_HARD                                = {10850, 10851, 10852, 10853};
    int[] VERZIK_VITUR_NORMAL                              = {8372, 8373, 8374, 8375};
    int[] VESPULA                                          = {7530, 7531, 7532};
    int[] VET_ION                                          = {6611, 6612};
    int[] VITREOUS_CHILLED_JELLY                           = {15501};
    int[] VITREOUS_JELLY                                   = {7399};
    int[] VITREOUS_WARPED_JELLY                            = {7400};
    int[] VOID_FLARE                                       = {14179};
    int[] VORKATH_AWAKENED                                 = {8058, 8059, 8060, 8061};

    // -----------------------------------------------------------------------
    // W
    // -----------------------------------------------------------------------
    int[] WALL_BEAST                                       = {476};
    int[] WARPED_JELLY                                     = {7277};
    int[] WARPED_TERRORBIRD                                = {12491, 12492, 12493, 12494, 12495, 12496, 12497, 12498, 12499, 12500, 12501, 12502, 12503, 12504};
    int[] WATERFIEND                                       = {2916, 2917};
    int[] WYRM                                             = {8610, 8611};

    // -----------------------------------------------------------------------
    // X
    // -----------------------------------------------------------------------
    int[] XARPUS_PHASE_2_3                                 = {8340, 10768, 10772};

    // -----------------------------------------------------------------------
    // Y
    // -----------------------------------------------------------------------
    int[] YAMA                                             = {14176, 15555};

    // -----------------------------------------------------------------------
    // Z
    // -----------------------------------------------------------------------
    int[] ZEBAK_MONSTER                                    = {11730, 11732};
    int[] ZULRAH                                           = {2044};
    int[] ZYGOMITE                                         = {537, 1024};

    // -----------------------------------------------------------------------
    // NPCs with null or removed IDs (no numeric ID available)
    // -----------------------------------------------------------------------
    // Cerberus (Echo)
    // Durial321
    // Dusk (Echo)
    // Hespori (Echo)
    // Kalphite Queen (Echo)
    // Thermonuclear smoke devil (Echo)
    // Unbound JalTok-Jad
}