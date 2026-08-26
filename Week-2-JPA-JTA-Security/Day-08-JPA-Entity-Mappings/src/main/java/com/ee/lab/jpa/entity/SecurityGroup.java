package com.ee.lab.jpa.entity;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "security_groups")
public class SecurityGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private Long id;

    @Column(name = "group_name", nullable = false, length = 64, unique = true)
    private String groupName;

    @Column(name = "firewall_policy", length = 128)
    private String firewallPolicy;

    @ManyToMany(mappedBy = "securityGroups", fetch = FetchType.LAZY)
    private Set<ClusterNode> nodes = new HashSet<>();

    public SecurityGroup() {
    }

    public SecurityGroup(String groupName, String firewallPolicy) {
        this.groupName = groupName;
        this.firewallPolicy = firewallPolicy;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public String getFirewallPolicy() { return firewallPolicy; }
    public void setFirewallPolicy(String firewallPolicy) { this.firewallPolicy = firewallPolicy; }

    public Set<ClusterNode> getNodes() { return nodes; }
    public void setNodes(Set<ClusterNode> nodes) { this.nodes = nodes; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SecurityGroup that = (SecurityGroup) o;
        return Objects.equals(groupName, that.groupName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupName);
    }
}
