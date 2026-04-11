package com.irene.irene.groups.repository;

import com.irene.irene.groups.model.GroupMembership;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupMembershipRepository extends JpaRepository<GroupMembership, Long> {
}
